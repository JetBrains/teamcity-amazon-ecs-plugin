package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudInstance

interface EcsCloudInstance : CloudInstance {
    val taskArn: String
    fun terminate()
}