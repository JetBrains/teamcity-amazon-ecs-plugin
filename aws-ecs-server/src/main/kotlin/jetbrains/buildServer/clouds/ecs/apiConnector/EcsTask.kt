/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package jetbrains.buildServer.clouds.ecs.apiConnector

import com.amazonaws.services.ecs.model.Task
import java.util.*

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 19.09.17.
 */
interface EcsTask {
    val arn: String
    val id: String
    val taskDefinitionArn: String
    val clusterArn: String?
    val lastStatus: String
    val desiredStatus: String
    val cratedAt: Date
    val startedAt: Date?
    fun getOverridenContainerEnv(envVarName: String): String?
}

fun Task.wrap(): EcsTask = object : EcsTask{
    override val id: String
        get() = this@wrap.taskArn.substring(this@wrap.taskArn.indexOf(":task/") + 6)
    override val taskDefinitionArn: String
        get() = this@wrap.taskDefinitionArn
    override val clusterArn: String?
        get() = this@wrap.clusterArn
    override val desiredStatus: String
        get() = this@wrap.desiredStatus
    override val lastStatus: String
        get() = this@wrap.lastStatus
    override val arn: String
        get() = this@wrap.taskArn
    override val cratedAt: Date
        get() = this@wrap.createdAt
    override val startedAt: Date?
        get() = this@wrap.startedAt

    override fun getOverridenContainerEnv(envVarName: String): String? {
        for(containerOverrides in this@wrap.overrides.containerOverrides){
            containerOverrides.environment
                    .filter { it.name.equals(envVarName) }
                    .forEach { return it.value }
        }
        return null
    }
}