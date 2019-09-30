package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudErrorInfo
import jetbrains.buildServer.clouds.InstanceStatus
import jetbrains.buildServer.serverSide.AgentDescription
import java.util.*

class BrokenEcsCloudInstance(private val instanceId: String,
                             private val cloudImage: EcsCloudImage,
                             private val errorInfo: CloudErrorInfo) : EcsCloudInstance {

    override val taskArn: String
        get() = ""

    private val startTime = Date()

    override fun terminate() {
        // do nothing
    }

    override fun getStatus() = InstanceStatus.ERROR

    override fun getInstanceId() = instanceId

    override fun getName() = instanceId

    override fun getStartedTime() = startTime

    override fun getImage() = cloudImage

    override fun getNetworkIdentity(): String? = null

    override fun getImageId() = cloudImage.id

    override fun getErrorInfo() = errorInfo

    override fun containsAgent(p0: AgentDescription) = false
}