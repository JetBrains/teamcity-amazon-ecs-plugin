package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudErrorInfo
import jetbrains.buildServer.clouds.InstanceStatus
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsTask
import jetbrains.buildServer.serverSide.AgentDescription
import java.util.*

class StartingEcsCloudInstance(private val instanceId: String,
                               private val cloudImage: EcsCloudImage
) : EcsCloudInstance {
    private val startTime = Date()
    @Volatile
    private var wasTerminated = false
    val terminateRequested : Boolean
        get() = wasTerminated


    override fun terminate() {
        wasTerminated = true
    }

    override fun update(task: EcsTask) {
        // do nothing
    }

    override fun getErrorInfo(): CloudErrorInfo? = null

    override fun getInstanceId() = instanceId

    override fun getName() = "Starting..."

    override fun getImageId() = cloudImage.id

    override fun getImage() = cloudImage

    override fun getStartedTime() = startTime

    override fun getNetworkIdentity() = null

    override fun getStatus() = InstanceStatus.SCHEDULED_TO_START

    override fun containsAgent(agent: AgentDescription) = false;
}