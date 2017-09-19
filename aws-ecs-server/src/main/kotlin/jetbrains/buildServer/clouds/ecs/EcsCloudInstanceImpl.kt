package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudErrorInfo
import jetbrains.buildServer.clouds.CloudImage
import jetbrains.buildServer.clouds.InstanceStatus
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsTask
import jetbrains.buildServer.serverSide.AgentDescription
import java.util.*

class EcsCloudInstanceImpl(val cloudImage: EcsCloudImage, val ecsTask: EcsTask, val apiConnector: EcsApiConnector) : EcsCloudInstance {
    private var myCurrentError: CloudErrorInfo? = null

    override fun getStatus(): InstanceStatus {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getInstanceId(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getName(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStartedTime(): Date {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getNetworkIdentity(): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun terminate() {
        apiConnector.stopTask(ecsTask.arn, null, "Terminated by TeamCity server")
    }
}