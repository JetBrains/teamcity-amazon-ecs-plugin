package jetbrains.buildServer.clouds.ecs

import com.amazonaws.services.ecs.model.LaunchType
import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.clouds.CloudErrorInfo
import jetbrains.buildServer.clouds.CloudException
import jetbrains.buildServer.clouds.CloudInstance
import jetbrains.buildServer.clouds.CloudInstanceUserData
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.serverSide.TeamCityProperties
import kotlinx.coroutines.*
import kotlin.concurrent.withLock
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock


class EcsCloudImageImpl(private val imageData: EcsCloudImageData,
                        private val apiConnector: EcsApiConnector,
                        private val cache: EcsDataCache,
                        private val serverUUID: String,
                        private val profileId: String) : EcsCloudImage {

    private val LOG = Logger.getInstance(EcsCloudImageImpl::class.java.getName())
    private val ERROR_INSTANCES_TIMEOUT: Long = 60*1000
    private val PENDING_INSTANCES_TIMEOUT: Long = 5*60*1000
    private val instanceIdLock = ReentrantLock()
    private val errorInstances = ConcurrentHashMap<String, Pair<EcsCloudInstance, Long>>()

    // ECS is eventually consistent: once we start an image, make it pending for up to 5 minutes
    // until we either see it via polling, or let go of it
    private val pendingInstances = ConcurrentHashMap<String, Pair<EcsCloudInstance, Long>>()

    private val muteTime = AtomicLong(0)


    override fun canStartNewInstance(): Boolean {
        if (System.currentTimeMillis() < muteTime.get())
            return false
        if(instanceLimit in 1..runningInstanceCount) return false
        val monitoringPeriod = TeamCityProperties.getInteger(ECS_METRICS_MONITORING_PERIOD, 1)
        return cpuReservationLimit <= 0 || apiConnector.getMaxCPUReservation(cluster, monitoringPeriod) < cpuReservationLimit
    }


    private val myIdToInstanceMap = ConcurrentHashMap<String, EcsCloudInstance>()
    private var myCurrentError: CloudErrorInfo? = null

    private val instanceLimit: Int
        get() = imageData.instanceLimit

    private val cpuReservationLimit: Int
        get() = imageData.cpuReservationLimit

    private val cluster: String?
        get() = imageData.cluster

    private val taskGroup: String?
        get() = imageData.taskGroup

    private val subnets: Collection<String>
        get() {
            val rawSubnetsString = imageData.subnets?.trim()
            return if(rawSubnetsString.isNullOrEmpty()) emptyList() else rawSubnetsString!!.lines()
        }

    private val securityGroups: Collection<String>
        get() {
            val rawSecurityGroupsString = imageData.securityGroups?.trim()
            return if(rawSecurityGroupsString.isNullOrEmpty()) emptyList() else rawSecurityGroupsString!!.lines()
        }

    private val assignPublicIp: Boolean
        get() = imageData.assignPublicIp

    private val launchType: String
        get() = imageData.launchType

    private val taskDefinition: String
        get() = imageData.taskDefinition

    override val runningInstanceCount: Int
        get() = myIdToInstanceMap.filterValues { instance -> instance.status.isStartingOrStarted }.size

    override fun getAgentPoolId(): Int? {
        return imageData.agentPoolId
    }

    override fun getName(): String {
        return taskDefinition
    }

    override fun getId(): String {
        return imageData.id
    }

    override fun getInstances(): MutableCollection<out CloudInstance> {
        return myIdToInstanceMap.values
    }

    override fun getErrorInfo(): CloudErrorInfo? {
        return myCurrentError
    }

    override fun findInstanceById(id: String): CloudInstance? {
        // Prevent a race when starting a new instance
        return instanceIdLock.withLock {
            val result = myIdToInstanceMap[id]
            if(result == null) {
                LOG.warn("Could not find cloud instance ${id} by id")
            }
            result
        }
    }

    override fun populateInstances() {
        try {
            expireErrorInstances()
            expirePendingInstances()

            val startedBy = startedByTeamCity(serverUUID)

            val runningTasks = apiConnector.listRunningTasks(cluster, startedBy).mapNotNull { taskArn -> apiConnector.describeTask(taskArn, cluster) }
            val stoppedTasks = apiConnector.listStoppedTasks(cluster, startedBy).mapNotNull { taskArn -> apiConnector.describeTask(taskArn, cluster) }

            // Ensure that all tasks known to ECS are retained in myIdToInstanceMap
            val toRetain = HashSet<String>()
            for (task in runningTasks.union(stoppedTasks)) {
                val taskProfileId = task.getOverridenContainerEnv(PROFILE_ID_ECS_ENV)
                val taskImageId = task.getOverridenContainerEnv(IMAGE_ID_ECS_ENV)
                if(profileId.equals(taskProfileId) && taskImageId.equals(id)){
                    val instanceId = task.getOverridenContainerEnv(INSTANCE_ID_ECS_ENV)
                    if(instanceId == null){
                        LOG.warn("Can't resolve cloud instance id of ecs task ${task.arn}")
                    } else {
                        cache.cleanInstanceStatus(task.arn)
                        toRetain.add(instanceId)
                        // This might be a task that was started by another instance of EcsCloudImageImpl (i.e. if a save of the profile
                        // caused new instances to be created, so make sure it's in the map
                        myIdToInstanceMap.putIfAbsent(instanceId, CachingEcsCloudInstance(EcsCloudInstanceImpl(instanceId, this, task, apiConnector), cache))
                    }
                }
            }
            LOG.debug("Found ${toRetain.size} tasks from ECS for profile ${profileId}:${id}")

            // And add all currently registered error instances
            errorInstances.forEach{
                toRetain.add(it.key)
            }

            // And add all currently registered pending instances
            // since the task may not have started running on ECS yet
            pendingInstances.forEach{
                toRetain.add(it.key)
            }

            // Purge stale entries from myIdToInstanceMap
            myIdToInstanceMap.keys.retainAll(toRetain)
            myCurrentError = null
        } catch (ex: Throwable) {
            val msg = "Unable to populate instances for ${imageData.id}"
            LOG.warnAndDebugDetails(msg, ex)
            myCurrentError = CloudErrorInfo(msg, ex.message.toString(), ex)
        }
    }


    @Synchronized
    override fun startNewInstance(tag: CloudInstanceUserData): EcsCloudInstance {
        var newInstance: EcsCloudInstance
        val launchType = LaunchType.valueOf(launchType)
        val instanceId = generateNewInstanceId()
        try {
            val taskDefinition = apiConnector.describeTaskDefinition(taskDefinition) ?: throw CloudException("""Task definition $taskDefinition is missing""")

            val additionalEnvironment = HashMap<String, String>()
            additionalEnvironment.put(SERVER_UUID_ECS_ENV, serverUUID)
            additionalEnvironment.put(SERVER_URL_ECS_ENV, tag.serverAddress)
            additionalEnvironment.put(OFFICIAL_IMAGE_SERVER_URL_ECS_ENV, tag.serverAddress)
            additionalEnvironment.put(PROFILE_ID_ECS_ENV, tag.profileId)
            additionalEnvironment.put(IMAGE_ID_ECS_ENV, id)
            additionalEnvironment.put(INSTANCE_ID_ECS_ENV, instanceId)
            additionalEnvironment.put(AGENT_NAME_ECS_ENV, generateAgentName(instanceId))

            for (pair in tag.customAgentConfigurationParameters){
                additionalEnvironment.put(TEAMCITY_ECS_PROVIDED_PREFIX + pair.key, pair.value)
            }

            // Lock here to prevent a race between the task starting on ECS (and hence triggering the agent registration code)
            // and recording the instance in the local state. This is not needed for the error case since there will be no
            // agent to call the server to register in that case.
            newInstance = instanceIdLock.withLock {
                val tasks = apiConnector.runTask(launchType, taskDefinition, cluster, taskGroup, subnets, securityGroups, assignPublicIp, additionalEnvironment, startedByTeamCity(serverUUID))
                newInstance = CachingEcsCloudInstance(EcsCloudInstanceImpl(instanceId, this, tasks[0], apiConnector), cache)
                myIdToInstanceMap[instanceId] = newInstance
                pendingInstances[instanceId] = Pair(newInstance, System.currentTimeMillis() + PENDING_INSTANCES_TIMEOUT)
                newInstance
            }
            LOG.info("Started new ECS instance ${instanceId} for profile ${profileId}:${id}")
        } catch (ex: Throwable){
            newInstance = BrokenEcsCloudInstance(instanceId, this, CloudErrorInfo(ex.message.toString(), ex.message.toString(), ex))
            errorInstances[instanceId] = Pair(newInstance, System.currentTimeMillis() + ERROR_INSTANCES_TIMEOUT)
            myIdToInstanceMap[instanceId] = newInstance
            muteTime.set(System.currentTimeMillis() + ERROR_INSTANCES_TIMEOUT)
        }
        return newInstance
    }

    override fun generateAgentName(instanceId: String): String {
        return imageData.agentNamePrefix + instanceId
    }

    private fun generateNewInstanceId(): String {
        val uniq = UUID.randomUUID().toString()
        return String.format("${imageData.taskDefinition}-${uniq}")
    }

    private fun expireErrorInstances() =  runBlocking {
        errorInstances.forEach{
            if (it.value.second < System.currentTimeMillis()) {
                errorInstances.remove(it.key)
            }
        }
    }

    private fun expirePendingInstances() =  runBlocking {
        pendingInstances.forEach{
            if (it.value.second < System.currentTimeMillis()) {
                pendingInstances.remove(it.key)
            }
        }
    }
}
