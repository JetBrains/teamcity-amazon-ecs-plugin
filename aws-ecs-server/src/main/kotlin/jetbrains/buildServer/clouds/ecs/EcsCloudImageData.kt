package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector

fun EcsCloudImageData.toImage(apiConnector: EcsApiConnector): EcsCloudImage  = EcsCloudImageImpl()

class EcsCloudImageData {

}