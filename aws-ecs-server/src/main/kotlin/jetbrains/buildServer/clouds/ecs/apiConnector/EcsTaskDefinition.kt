

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