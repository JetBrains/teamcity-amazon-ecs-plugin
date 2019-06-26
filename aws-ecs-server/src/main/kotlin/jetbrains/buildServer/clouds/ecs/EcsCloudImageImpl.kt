package jetbrains.buildServer.clouds.ecs

import com.amazonaws.services.ecs.model.LaunchType
import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.agent.AgentRuntimeProperties.AGENT_NAME
import jetbrains.buildServer.clouds.CloudErrorInfo
import jetbrains.buildServer.clouds.CloudException
import jetbrains.buildServer.clouds.CloudInstance
import jetbrains.buildServer.clouds.CloudInstanceUserData
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.messages.serviceMessages.MapSerializerUtil
import jetbrains.buildServer.serverSide.TeamCityProperties
import jetbrains.buildServer.util.FileUtil
import jetbrains.buildServer.util.StringUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class EcsCloudImageImpl(private val imageData: EcsCloudImageData,
                        private val apiConnector: EcsApiConnector,
                        private val cache: EcsDataCache,
                        private val serverUUID: String,
                        idxStorage: File,
                        private val profileId: String) : EcsCloudImage {
    private val idxFile = File(idxStorage, imageName4File() + ".idx")
    private val idxCounter = AtomicInteger(0)
    private val idxTouched = AtomicBoolean(false)
    private val mutex = Mutex()
    private val LOG = Logger.getInstance(EcsCloudImageImpl::class.java.getName())
    private val counterContext = newSingleThreadContext("IdxContext")


    init{
        try {
            if (!idxFile.exists()) {
                idxCounter.set(1)
                idxTouched.set(true)
                storeIdx()
            } else {
                runBlocking {
                    mutex.withLock {
                        idxCounter.set(Integer.parseInt(FileUtil.readText(idxFile)))
                    }
                }
            }
        } catch (ex: Exception) {
            LOG.warnAndDebugDetails("Unable to process idx file '${idxFile.absolutePath}'. Will reset the index for ${imageData.taskDefinition}", ex)
            idxCounter.set(1)
        }

        GlobalScope.async{
            while (true) {
                try {
                    storeIdx()
                    delay(1000)
                } catch (ex: Exception){

                }
            }
        }
    }

    override fun canStartNewInstance(): Boolean {
        if(instanceLimit in 1..runningInstanceCount) return false
        val monitoringPeriod = TeamCityProperties.getInteger(ECS_METRICS_MONITORING_PERIOD, 1)
        return cpuReservalionLimit <= 0 || apiConnector.getMaxCPUReservation(cluster, monitoringPeriod) < cpuReservalionLimit
    }


    private val myIdToInstanceMap = ConcurrentHashMap<String, EcsCloudInstance>()
    private var myCurrentError: CloudErrorInfo? = null

    private val instanceLimit: Int
        get() = imageData.instanceLimit

    private val cpuReservalionLimit: Int
        get() = imageData.cpuReservalionLimit

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
        return myIdToInstanceMap[id]
    }

    override fun populateInstances() {
        try {
            val startedBy = startedByTeamCity(serverUUID)
            val runningTasks = apiConnector.listRunningTasks(cluster, startedBy).mapNotNull { taskArn -> apiConnector.describeTask(taskArn, cluster) }
            val stoppedTasks = apiConnector.listStoppedTasks(cluster, startedBy).mapNotNull { taskArn -> apiConnector.describeTask(taskArn, cluster) }

            synchronized(myIdToInstanceMap, {
                myIdToInstanceMap.clear()
                for (task in runningTasks.union(stoppedTasks)) {
                    val taskProfileId = task.getOverridenContainerEnv(PROFILE_ID_ECS_ENV)
                    val taskImageId = task.getOverridenContainerEnv(IMAGE_ID_ECS_ENV)
                    if(profileId.equals(taskProfileId) && taskImageId.equals(id)){
                        val instanceId = task.getOverridenContainerEnv(INSTANCE_ID_ECS_ENV)
                        if(instanceId == null){
                            LOG.warn("Can't resolve cloud instance id of ecs task ${task.arn}")
                        } else {
                            cache.cleanInstanceStatus(task.arn)
                            myIdToInstanceMap.put(instanceId, CachingEcsCloudInstance(EcsCloudInstanceImpl(instanceId, this, task, apiConnector), cache))
                        }
                    }
                }
                myCurrentError = null
            })
        } catch (ex: Throwable) {
            val msg = "Unable to populate instances for ${imageData.id}"
            LOG.warnAndDebugDetails(msg, ex)
            myCurrentError = CloudErrorInfo(msg, ex.message.toString(), ex)
        }
    }


    @Synchronized
    override fun startNewInstance(tag: CloudInstanceUserData): EcsCloudInstance {
        val newInstance: CachingEcsCloudInstance
        try {
            val launchType = LaunchType.valueOf(launchType)
            val taskDefinition = apiConnector.describeTaskDefinition(taskDefinition) ?: throw CloudException("""Task definition $taskDefinition is missing""")
            val instanceId = generateNewInstanceId()

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

            val tasks = apiConnector.runTask(launchType, taskDefinition, cluster, taskGroup, subnets, securityGroups, assignPublicIp, additionalEnvironment, startedByTeamCity(serverUUID))

            newInstance = CachingEcsCloudInstance(EcsCloudInstanceImpl(instanceId, this, tasks[0], apiConnector), cache)
        } catch (ex: Throwable){
            myCurrentError = CloudErrorInfo("Failed start new instance", ex.message.toString(), ex)
            throw ex
        }
        populateInstances()
        return newInstance
    }

    override fun generateAgentName(instanceId: String): String {
        return imageData.agentNamePrefix + instanceId
    }

    private fun generateNewInstanceId(): String {
        lateinit var retval : String
        do{
            retval = String.format("${imageData.taskDefinition}-${idxCounter.getAndIncrement()}")
        } while (myIdToInstanceMap.containsKey(retval))
        LOG.info("Will create a new instance with name $retval")
        return retval
    }

    private fun imageName4File():String {
        return StringUtil.replaceNonAlphaNumericChars(imageData.taskDefinition, '_')
    }

    private fun storeIdx() = runBlocking {
        if (idxTouched.compareAndSet(true, false)){
            mutex.withLock {
                FileUtil.writeViaTmpFile(idxFile, ByteArrayInputStream(idxCounter.get().toString().toByteArray()),
                        FileUtil.IOAction.DO_NOTHING)
            }
        }
    }
}
