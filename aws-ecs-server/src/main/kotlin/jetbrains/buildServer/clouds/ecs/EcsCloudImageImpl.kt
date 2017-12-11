package jetbrains.buildServer.clouds.ecs

import com.amazonaws.services.ecs.model.LaunchType
import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.clouds.CloudErrorInfo
import jetbrains.buildServer.clouds.CloudException
import jetbrains.buildServer.clouds.CloudInstance
import jetbrains.buildServer.clouds.CloudInstanceUserData
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.serverSide.TeamCityProperties

class EcsCloudImageImpl(private val imageData: EcsCloudImageData,
                        private val apiConnector: EcsApiConnector,
                        private val cache: EcsDataCache,
                        private val serverUUID: String, private val profileId: String) : EcsCloudImage {

    override fun canStartNewInstance(): Boolean {
        if(instanceLimit in 1..runningInstanceCount) return false
        val monitoringPeriod = TeamCityProperties.getInteger(ECS_METRICS_MONITORING_PERIOD, 1)
        return cpuReservalionLimit <= 0 || apiConnector.getMaxCPUReservation(cluster, monitoringPeriod) < cpuReservalionLimit
    }

    private val LOG = Logger.getInstance(EcsCloudImageImpl::class.java.getName())

    private val myIdToInstanceMap = HashMap<String, EcsCloudInstance>()
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
            myCurrentError = CloudErrorInfo("Failed populate instances", ex.message.toString(), ex)
            throw ex
        }
    }


    @Synchronized
    override fun startNewInstance(tag: CloudInstanceUserData): EcsCloudInstance {
        val launchType = LaunchType.valueOf(launchType)
        val taskDefinition = apiConnector.describeTaskDefinition(taskDefinition) ?: throw CloudException("""Task definition $taskDefinition is missing""")
        val instanceId = generateNewInstanceId(taskDefinition.family, myIdToInstanceMap.keys)

        val additionalEnvironment = HashMap<String, String>()
        additionalEnvironment.put(SERVER_UUID_ECS_ENV, serverUUID)
        additionalEnvironment.put(SERVER_URL_ECS_ENV, tag.serverAddress)
        additionalEnvironment.put(OFFICIAL_IMAGE_SERVER_URL_ECS_ENV, tag.serverAddress)
        additionalEnvironment.put(PROFILE_ID_ECS_ENV, tag.profileId)
        additionalEnvironment.put(IMAGE_ID_ECS_ENV, id)
        additionalEnvironment.put(INSTANCE_ID_ECS_ENV, instanceId)
        additionalEnvironment.put(AGENT_NAME_ECS_ENV, generateAgentName(instanceId))

        val tasks = apiConnector.runTask(launchType, taskDefinition, cluster, taskGroup, subnets, additionalEnvironment, startedByTeamCity(serverUUID))
        val newInstance = CachingEcsCloudInstance(EcsCloudInstanceImpl(instanceId, this, tasks[0], apiConnector), cache)
        populateInstances()
        return newInstance
    }

    override fun generateAgentName(instanceId: String): String {
        return imageData.agentNamePrefix + instanceId
    }

    private fun generateNewInstanceId(prefix: String, currentIds: Collection<String>): String {
        var counter = 1
        var newId = "$prefix-${counter}"
        while (currentIds.contains(newId)){
            counter++
            newId = "$prefix-${counter}"
        }
        return newId
    }
}
