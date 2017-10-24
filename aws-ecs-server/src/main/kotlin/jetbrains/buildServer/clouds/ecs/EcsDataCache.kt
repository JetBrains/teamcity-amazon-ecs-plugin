package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.InstanceStatus

interface EcsDataCache {
    fun getInstanceStatus(taskArn: String, resolver: () -> InstanceStatus): InstanceStatus
}