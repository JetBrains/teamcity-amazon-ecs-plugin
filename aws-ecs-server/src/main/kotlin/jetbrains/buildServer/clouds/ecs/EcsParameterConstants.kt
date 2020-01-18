/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudImageParameters

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 21.09.17.
 */

val PROFILE_INSTANCE_LIMIT_PARAM = "profileInstanceLimit"
val IMAGE_INSTANCE_LIMIT_PARAM = "maxInstances"
val CPU_RESERVATION_LIMIT_PARAM = "cpuReservationLimit"
val LAUNCH_TYPE_PARAM = "launchType"
val TASK_DEFINITION_PARAM = "taskDefinition"
val TASK_GROUP_PARAM = "taskGroup"
val SUBNETS_PARAM = "subnets"
val SECURITY_GROUPS_PARAM = "securityGroups"
val ASSIGN_PUBLIC_IP_PARAM = "assignPublicIp"
val CLUSTER_PARAM = "cluster"
val AGENT_NAME_PREFIX = "agentNamePrefix"

class EcsParameterConstants{
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
}