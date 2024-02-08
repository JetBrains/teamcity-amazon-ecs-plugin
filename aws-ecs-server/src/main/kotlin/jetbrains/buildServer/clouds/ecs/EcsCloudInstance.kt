

package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudInstance
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsTask

interface EcsCloudInstance : CloudInstance {
    fun terminate()

    fun update(task: EcsTask)
}