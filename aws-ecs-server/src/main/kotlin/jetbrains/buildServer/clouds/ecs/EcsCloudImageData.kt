package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudImageParameters
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector

fun EcsCloudImageData.toImage(apiConnector: EcsApiConnector): EcsCloudImage  = EcsCloudImageImpl(this, apiConnector)

class EcsCloudImageData(private val generalImageParams: CloudImageParameters) {

}