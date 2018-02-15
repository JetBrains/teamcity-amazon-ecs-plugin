package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudErrorInfo
import jetbrains.buildServer.clouds.CloudImage
import jetbrains.buildServer.clouds.InstanceStatus
import jetbrains.buildServer.serverSide.AgentDescription
import java.util.*

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 23.10.17.
 */
class CachingEcsCloudInstance(val inner: EcsCloudInstance, val cache: EcsDataCache) : EcsCloudInstance {
    override val taskArn: String
        get() = inner.taskArn

    override fun terminate() {
        inner.terminate()
        cache.cleanInstanceStatus(taskArn)
    }

    override fun getStatus(): InstanceStatus {
        return cache.getInstanceStatus(inner.taskArn, { inner.status })
    }

    override fun getInstanceId(): String {
        return inner.instanceId
    }

    override fun getName(): String {
        return inner.name
    }

    override fun getStartedTime(): Date {
        return inner.startedTime
    }

    override fun getImage(): CloudImage {
        return inner.image
    }

    override fun getNetworkIdentity(): String? {
        return inner.networkIdentity
    }

    override fun getImageId(): String {
        return inner.imageId
    }

    override fun getErrorInfo(): CloudErrorInfo? {
        return inner.errorInfo
    }

    override fun containsAgent(agent: AgentDescription): Boolean {
        return inner.containsAgent(agent)
    }
}