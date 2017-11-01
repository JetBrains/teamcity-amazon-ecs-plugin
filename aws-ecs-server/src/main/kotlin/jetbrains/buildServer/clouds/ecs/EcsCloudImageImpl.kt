package jetbrains.buildServer.clouds.ecs

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.clouds.CloudErrorInfo
import jetbrains.buildServer.clouds.CloudInstance
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.serverSide.TeamCityProperties
import java.util.concurrent.ConcurrentHashMap

class EcsCloudImageImpl(private val imageData: EcsCloudImageData,
                        private val apiConnector: EcsApiConnector,
                        private val cache: EcsDataCache) : EcsCloudImage {
    override fun canStartNewInstance(): Boolean {
        if(instanceLimit > 0 && instanceCount >= instanceLimit) return false
        val monitoringPeriod = TeamCityProperties.getInteger("teamcity.ecs.cluster.monitoring.period", 60 * 5)
        return memoryReservalionLimit <= 0 || apiConnector.getAverageMemoryReservation(cluster, monitoringPeriod) < memoryReservalionLimit
    }

    private val LOG = Logger.getInstance(EcsCloudImageImpl::class.java.getName())

    private val myIdToInstanceMap = ConcurrentHashMap<String, EcsCloudInstance>()
    private var myCurrentError: CloudErrorInfo? = null

    private val instanceLimit: Int
        get() = imageData.instanceLimit

    private val memoryReservalionLimit: Int
        get() = imageData.memoryReservalionLimit

    override val instanceCount: Int
        get() = myIdToInstanceMap.size

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

    override fun populateInstances(startedBy: String) {
        try {
            val tasks = apiConnector.listTasks(cluster, startedBy)
                    .map { taskArn -> apiConnector.describeTask(taskArn, cluster) }
                    .filterNotNull()

            for (task in tasks) {
                val instanceId = task.getOverridenContainerEnv(INSTANCE_ID_ECS_ENV)
                if(instanceId == null){
                    LOG.warn("Can't resolve cloud instance id of ecs task ${task.arn}")
                } else {
                    myIdToInstanceMap.put(instanceId, CachingEcsCloudInstance(EcsCloudInstanceImpl(instanceId, this, task, apiConnector), cache))
                }
            }
            myCurrentError = null
        } catch (ex: Throwable) {
            myCurrentError = CloudErrorInfo("Failed populate instances", ex.message.toString(), ex)
            throw ex
        }

    }

    override fun generateAgentName(instanceId: String): String {
        return imageData.agentNamePrefix + instanceId
    }

    override fun addInstance(instance: EcsCloudInstance) {
        myIdToInstanceMap.put(instance.instanceId, instance)
    }

    override fun deleteInstance(instance: EcsCloudInstance) {
        myIdToInstanceMap.remove(instance.instanceId)
    }
}
