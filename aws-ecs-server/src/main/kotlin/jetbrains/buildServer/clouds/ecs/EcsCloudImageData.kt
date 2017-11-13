package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudImageParameters
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.util.StringUtil

fun EcsCloudImageData.toImage(apiConnector: EcsApiConnector,
                              cache: EcsDataCache,
                              serverUUID: String): EcsCloudImage  = EcsCloudImageImpl(this, apiConnector, cache, serverUUID)

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

    val cpuReservalionLimit: Int
        get() {
            val parameter = rawImageData.getParameter(CPU_RESERVATION_LIMIT_PARAM)
            return if (StringUtil.isEmpty(parameter)) -1 else Integer.valueOf(parameter)
        }

    val agentNamePrefix: String
        get() {
            val prefix = rawImageData.getParameter(AGENT_NAME_PREFIX)
            if(prefix == null || prefix.isEmpty()) return "ecs:"
            else return prefix
        }
}