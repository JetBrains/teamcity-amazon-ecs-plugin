

package jetbrains.buildServer.clouds.ecs.apiConnector

import com.amazonaws.services.ecs.model.Failure

class EcsApiCallFailureException(failures: MutableList<Failure>) : Exception(failures.map { it.toString() }.joinToString("\n")) {
}