package jetbrains.buildServer.clouds.ecs.apiConnector

import com.amazonaws.services.ecs.model.TaskDefinition

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 19.09.17.
 */
interface EcsTaskDefinition {
    val arn: String
    val containers: Collection<String>
}

fun TaskDefinition.wrap(): EcsTaskDefinition = object : EcsTaskDefinition{
    override val containers: Collection<String>
        get() = this@wrap.containerDefinitions.map { containerDef -> containerDef.name }
    override val arn: String
        get() = this@wrap.taskDefinitionArn
}