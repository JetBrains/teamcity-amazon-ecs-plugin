package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.InstanceStatus
import java.util.concurrent.ConcurrentHashMap

class EcsDataCacheImpl : EcsDataCache {
    private val cache: MutableMap<String, InstanceStatus> = ConcurrentHashMap()

    override fun getInstanceStatus(taskArn: String, resolver: () -> InstanceStatus): InstanceStatus {
        return cache.getOrPut(taskArn, resolver)
    }
}