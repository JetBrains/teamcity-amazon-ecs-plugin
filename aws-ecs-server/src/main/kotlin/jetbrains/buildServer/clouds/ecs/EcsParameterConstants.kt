package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudImageParameters

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 21.09.17.
 */

val PROFILE_INSTANCE_LIMIT_PARAM = "profileInstanceLimit"
val PROFILE_REGION = "profileInstanceLimit"
val IMAGE_INSTANCE_LIMIT_PARAM = "imageInstanceLimit"
val TASK_DEFINITION_PARAM = "ecsTaskDefinition"
val TASK_GROUP_PARAM = "ecsTaskGroup"
val CLUSTER_PARAM = "ecsCluster"

class EcsParameterConstants{

    val taskDefinition: String
        get() {
            return TASK_DEFINITION_PARAM
        }

    val cluster: String
        get() {
            return CLUSTER_PARAM
        }

    val taskGroup: String
        get() {
            return TASK_GROUP_PARAM
        }

    val maxInstances: String
        get() {
            return IMAGE_INSTANCE_LIMIT_PARAM
        }

    val agentPoolIdField: String
        get() {
            return CloudImageParameters.AGENT_POOL_ID_FIELD
        }
}