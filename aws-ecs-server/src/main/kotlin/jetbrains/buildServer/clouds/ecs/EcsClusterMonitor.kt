package jetbrains.buildServer.clouds.ecs

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 01.11.17.
 */
interface EcsClusterMonitor {
    fun clusterHasAvailableResources(): Boolean
}