package jetbrains.buildServer.clouds.ecs.apiConnector

import com.amazonaws.services.ecs.model.TaskDefinition

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 19.09.17.
 */
interface EcsTaskDefinition {
}

fun TaskDefinition.wrap(): EcsTaskDefinition = object : EcsTaskDefinition{}