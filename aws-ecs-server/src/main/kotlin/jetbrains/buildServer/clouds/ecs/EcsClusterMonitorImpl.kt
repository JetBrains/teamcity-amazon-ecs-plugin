package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector

class EcsClusterMonitorImpl(private val ecsParams: EcsCloudClientParameters,
                            private val apiConnector: EcsApiConnector) : EcsClusterMonitor {
    override fun clusterHasAvailableResources(): Boolean {
        return true
    }
}