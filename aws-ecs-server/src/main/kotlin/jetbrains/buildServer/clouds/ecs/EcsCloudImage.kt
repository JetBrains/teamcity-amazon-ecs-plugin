

package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CanStartNewInstanceResult
import jetbrains.buildServer.clouds.CloudImage
import jetbrains.buildServer.clouds.CloudInstanceUserData

interface EcsCloudImage : CloudImage {
    val runningInstanceCount: Int

    fun populateInstances()
    fun generateAgentName(instanceId: String): String

    fun canStartNewInstanceWithDetails(): CanStartNewInstanceResult
    fun startNewInstance(tag: CloudInstanceUserData): EcsCloudInstance
}