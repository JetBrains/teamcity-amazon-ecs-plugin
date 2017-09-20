package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudImage

interface EcsCloudImage : CloudImage {
    val taskDefinition: String
    val cluster: String?
    val taskGroup: String?
}