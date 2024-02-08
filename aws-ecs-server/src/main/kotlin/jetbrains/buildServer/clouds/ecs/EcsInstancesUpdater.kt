

package jetbrains.buildServer.clouds.ecs

interface EcsInstancesUpdater {
    fun registerClient(client: EcsCloudClient)
    fun unregisterClient(client: EcsCloudClient)
}