package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.ecs.apiConnector.EcsTaskDefinition
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 29.09.17.
 */

private val STARTING_INSTANCE_IDX = AtomicInteger(0)

fun EcsTaskDefinition.generateInstanceId(): String {
    return this.family + STARTING_INSTANCE_IDX.incrementAndGet()
}
