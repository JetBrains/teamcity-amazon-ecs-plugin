package jetbrains.buildServer.clouds.ecs.apiConnector

import com.amazonaws.services.ecs.model.Task

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 19.09.17.
 */
interface EcsTask {
    val arn: String
}

fun Task.wrap(): EcsTask = object : EcsTask{
    override val arn: String
        get() = this.arn
}