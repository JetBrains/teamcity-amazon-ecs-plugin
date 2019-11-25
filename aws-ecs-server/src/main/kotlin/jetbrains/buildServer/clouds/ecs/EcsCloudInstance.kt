package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudInstance
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsTask

interface EcsCloudInstance : CloudInstance {
    val taskArn: String
    fun terminate()

    fun update(task: EcsTask)
}