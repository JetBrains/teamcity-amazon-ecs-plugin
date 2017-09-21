package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudImage

interface EcsCloudImage : CloudImage {
    val taskDefinition: String
    val cluster: String?
    val taskGroup: String?
    val instanceLimit: Int
    val instanceCount: Int

    fun populateInstances()
    fun deleteInstance(instance: EcsCloudInstance)
}