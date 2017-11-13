package jetbrains.buildServer.clouds.ecs

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.clouds.CloudErrorInfo
import jetbrains.buildServer.clouds.CloudInstance
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.serverSide.TeamCityProperties

class EcsCloudImageImpl(private val imageData: EcsCloudImageData,
                        private val apiConnector: EcsApiConnector,
                        private val cache: EcsDataCache,
                        private val serverUUID: String) : EcsCloudImage {
    override fun canStartNewInstance(): Boolean {
        if(instanceLimit > 0 && runningInstanceCount >= instanceLimit) return false
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

    override val runningInstanceCount: Int
        get() = myIdToInstanceMap.filterValues { instance -> instance.status.isStartingOrStarted }.size

    override val taskDefinition: String
        get() = imageData.taskDefinition

    override val cluster: String?
        get() = imageData.cluster

    override val taskGroup: String?
        get() = imageData.taskGroup

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
            val tasks = apiConnector.listTasks(cluster, startedByTeamCity(serverUUID))
                    .map { taskArn -> apiConnector.describeTask(taskArn, cluster) }
                    .filterNotNull()

            synchronized(myIdToInstanceMap, {
                myIdToInstanceMap.clear()
                for (task in tasks) {
                    val instanceId = task.getOverridenContainerEnv(INSTANCE_ID_ECS_ENV)
                    if(instanceId == null){
                        LOG.warn("Can't resolve cloud instance id of ecs task ${task.arn}")
                    } else {
                        cache.cleanInstanceStatus(task.arn);
                        myIdToInstanceMap.put(instanceId, CachingEcsCloudInstance(EcsCloudInstanceImpl(instanceId, this, task, apiConnector), cache))
                    }
                }
                myCurrentError = null
            })
        } catch (ex: Throwable) {
            myCurrentError = CloudErrorInfo("Failed populate instances", ex.message.toString(), ex)
            throw ex
        }
    }

    override fun generateAgentName(instanceId: String): String {
        return imageData.agentNamePrefix + instanceId
    }
}
