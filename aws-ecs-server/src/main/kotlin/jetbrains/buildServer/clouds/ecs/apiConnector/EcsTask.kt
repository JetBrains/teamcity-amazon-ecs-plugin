package jetbrains.buildServer.clouds.ecs.apiConnector

import com.amazonaws.services.ecs.model.Task

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 19.09.17.
 */
interface EcsTask {
    val arn: String
    val lastStatus: String
    val desiredStatus: String
}

fun Task.wrap(): EcsTask = object : EcsTask{
    override val desiredStatus: String
        get() = this@wrap.desiredStatus
    override val lastStatus: String
        get() = this@wrap.lastStatus
    override val arn: String
        get() = this@wrap.taskArn
}