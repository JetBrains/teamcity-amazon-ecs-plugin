

package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudImageParameters

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 21.09.17.
 */

const val PROFILE_INSTANCE_LIMIT_PARAM = "profileInstanceLimit"
const val IMAGE_INSTANCE_LIMIT_PARAM = "maxInstances"
const val CPU_RESERVATION_LIMIT_PARAM = "cpuReservationLimit"
const val LAUNCH_TYPE_PARAM = "launchType"
const val TASK_DEFINITION_PARAM = "taskDefinition"
const val TASK_GROUP_PARAM = "taskGroup"
const val SUBNETS_PARAM = "subnets"
const val SECURITY_GROUPS_PARAM = "securityGroups"
const val ASSIGN_PUBLIC_IP_PARAM = "assignPublicIp"
const val CLUSTER_PARAM = "cluster"
const val AGENT_NAME_PREFIX = "agentNamePrefix"
const val FARGATE_PLATFORM_VERSION = "fargatePlatformVersion"

class EcsParameterConstants{
    companion object{
        val FARGATE_VERSIONS = arrayOf(
                "LATEST",
                "1.4.0",
                "1.3.0",
                "1.2.0",
                "1.1.0",
                "1.0.0"
        )
    }

    val agentNamePrefix: String = AGENT_NAME_PREFIX
    val launchType: String = LAUNCH_TYPE_PARAM
    val taskDefinition: String = TASK_DEFINITION_PARAM
    val cluster: String = CLUSTER_PARAM
    val taskGroup: String = TASK_GROUP_PARAM
    val subnets: String = SUBNETS_PARAM
    val securityGroups: String = SECURITY_GROUPS_PARAM
    val assignPublicIp: String = ASSIGN_PUBLIC_IP_PARAM
    val maxInstances: String = IMAGE_INSTANCE_LIMIT_PARAM
    val agentPoolIdField: String = CloudImageParameters.AGENT_POOL_ID_FIELD
    val profileInstanceLimit: String = PROFILE_INSTANCE_LIMIT_PARAM
    val cpuReservationLimit: String = CPU_RESERVATION_LIMIT_PARAM
    val fargatePlatformVersion = FARGATE_PLATFORM_VERSION
}