package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.agent.Constants
import jetbrains.buildServer.clouds.CloudErrorInfo
import jetbrains.buildServer.clouds.CloudImage
import jetbrains.buildServer.clouds.InstanceStatus
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiCallFailureException
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsTask
import jetbrains.buildServer.serverSide.AgentDescription
import java.util.*

class EcsCloudInstanceImpl(private val instanceId: String, val cloudImage: EcsCloudImage, val ecsTask: EcsTask, val apiConnector: EcsApiConnector) : EcsCloudInstance {
    private var myCurrentError: CloudErrorInfo? = null

    override fun getStatus(): InstanceStatus {
        val task: EcsTask?
        try{
            task = apiConnector.describeTask(ecsTask.arn, ecsTask.clusterArn)
        } catch (ex: EcsApiCallFailureException){
            return InstanceStatus.UNKNOWN
        }
        if(task == null) return InstanceStatus.UNKNOWN
        val lastStatus = task.lastStatus
        val desiredStatus = task.desiredStatus
        when (desiredStatus) {
            "RUNNING" -> {
                when(lastStatus){
                    "PENDING" -> return InstanceStatus.STARTING
                    "RUNNING" -> return InstanceStatus.RUNNING
                    else -> return InstanceStatus.RUNNING
                }
            }
            "STOPPED" -> {
                when(lastStatus){
                    "RUNNING" -> return InstanceStatus.STOPPING
                    "PENDING" -> return InstanceStatus.STOPPED
                    "STOPPED" -> return InstanceStatus.STOPPED
                    else -> return InstanceStatus.STOPPED
                }
            }
            else -> return InstanceStatus.UNKNOWN
        }
    }

    override fun getInstanceId(): String {
        return instanceId
    }

    override fun getName(): String {
        return ecsTask.id
    }

    override fun generateAgentName(): String {
        return cloudImage.generateAgentName(instanceId)
    }

    override fun getStartedTime(): Date {
        val startedAt = ecsTask.startedAt
        when {
            startedAt != null -> return startedAt
            else -> return ecsTask.cratedAt
        }
    }

    override fun getNetworkIdentity(): String? {
        //TODO: provide identity
        return null
    }

    override fun getImage(): CloudImage {
        return cloudImage
    }

    override fun getImageId(): String {
        return cloudImage.id
    }

    override fun getErrorInfo(): CloudErrorInfo? {
        return myCurrentError
    }

    override fun containsAgent(agent: AgentDescription): Boolean {
        return instanceId == agent.configurationParameters.get(Constants.ENV_PREFIX + INSTANCE_ID_ECS_ENV)
    }

    override fun terminate() {
        apiConnector.stopTask(ecsTask.arn, ecsTask.clusterArn, "Terminated by TeamCity server")
        myCurrentError = null
        cloudImage.deleteInstance(this)
    }
}
