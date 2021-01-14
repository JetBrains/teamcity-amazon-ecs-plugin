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

import com.amazonaws.services.ecs.model.Compatibility
import com.amazonaws.services.ecs.model.TaskDefinition

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 19.09.17.
 */
interface EcsTaskDefinition {
    val arn: String
    val displayName: String
    val family: String
    val containers: Collection<String>
    val requiresCompatibilitiesString: String
    fun isCompatibleWithLaunchType(launchType: String?): Boolean
}

fun TaskDefinition.wrap(): EcsTaskDefinition = object : EcsTaskDefinition{
    override fun isCompatibleWithLaunchType(launchType: String?): Boolean {
        if(launchType.isNullOrEmpty()) return true

        var requiresCompatibilities = this@wrap.requiresCompatibilities
        if(requiresCompatibilities.isEmpty()) requiresCompatibilities = listOf(Compatibility.EC2.name)

        return requiresCompatibilities.contains(launchType)
    }

    override val requiresCompatibilitiesString: String
        get(){
            var requiresCompatibilities = this@wrap.requiresCompatibilities
            if(requiresCompatibilities.isEmpty()) requiresCompatibilities = listOf(Compatibility.EC2.name)
            return requiresCompatibilities.joinToString(separator = " ,")
        }

    override val displayName: String
        get() = "${this@wrap.family}:${this@wrap.revision}"

    override val family: String
        get() = this@wrap.family

    override val containers: Collection<String>
        get() = this@wrap.containerDefinitions.map { containerDef -> containerDef.name }

    override val arn: String
        get() = this@wrap.taskDefinitionArn
}