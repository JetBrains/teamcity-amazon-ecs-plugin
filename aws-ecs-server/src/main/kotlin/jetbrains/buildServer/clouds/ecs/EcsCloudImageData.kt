

package jetbrains.buildServer.clouds.ecs

import com.amazonaws.services.ecs.model.LaunchType
import jetbrains.buildServer.clouds.CloudImageParameters
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.util.StringUtil
import java.io.File

fun EcsCloudImageData.toImage(apiConnector: EcsApiConnector,
                              serverUUID: String,
                              idxStorage: File,
                              profileId: String): EcsCloudImage
        = EcsCloudImageImpl(this, apiConnector, serverUUID, idxStorage, profileId)

class EcsCloudImageData(private val rawImageData: CloudImageParameters) {
    val id: String = rawImageData.id!!
    val agentPoolId: Int? = rawImageData.agentPoolId
    val taskGroup: String? = rawImageData.getParameter(TASK_GROUP_PARAM)
    val subnets: String? = rawImageData.getParameter(SUBNETS_PARAM)
    val securityGroups: String? = rawImageData.getParameter(SECURITY_GROUPS_PARAM)
    val cluster: String? = rawImageData.getParameter(CLUSTER_PARAM)
    val taskDefinition: String = rawImageData.getParameter(TASK_DEFINITION_PARAM)!!
    val fargatePlatformVersion: String? = rawImageData.getParameter(FARGATE_PLATFORM_VERSION)

    val launchType: String?
        get() {
            val parameter = rawImageData.getParameter(LAUNCH_TYPE_PARAM)
            return if(parameter.isNullOrEmpty()) null else parameter
        }

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

    val assignPublicIp: Boolean
        get() = rawImageData.getParameter(ASSIGN_PUBLIC_IP_PARAM)?.toBoolean() ?: false
}