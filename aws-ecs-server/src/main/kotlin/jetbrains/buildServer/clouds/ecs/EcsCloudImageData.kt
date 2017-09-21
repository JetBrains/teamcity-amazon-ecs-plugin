package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudImageParameters
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.util.StringUtil

fun EcsCloudImageData.toImage(apiConnector: EcsApiConnector): EcsCloudImage  = EcsCloudImageImpl(this, apiConnector)

class EcsCloudImageData(private val rawImageData: CloudImageParameters) {
    val id: String = rawImageData.id!!
    val agentPoolId: Int? = rawImageData.agentPoolId
    val taskGroup: String? = rawImageData.getParameter(TASK_GROUP_PARAM)
    val cluster: String? = rawImageData.getParameter(CLUSTER_PARAM)
    val taskDefinition: String = rawImageData.getParameter(TASK_DEFINITION_PARAM)!!
    val instanceLimit: Int
        get() {
            val parameter = rawImageData.getParameter(IMAGE_INSTANCE_LIMIT_PARAM)
            return if (StringUtil.isEmpty(parameter)) -1 else Integer.valueOf(parameter)
        }
}