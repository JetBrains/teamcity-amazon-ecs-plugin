package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudImage

interface EcsCloudImage : CloudImage {
    val taskDefinition: String
    val cluster: String?
    val taskGroup: String?

    val instanceCount: Int

    fun addInstance(instance: EcsCloudInstance)
    fun deleteInstance(instance: EcsCloudInstance)
    fun populateInstances(startedBy:String)
    fun generateAgentName(instanceId: String): String

    fun canStartNewInstance(): Boolean
}