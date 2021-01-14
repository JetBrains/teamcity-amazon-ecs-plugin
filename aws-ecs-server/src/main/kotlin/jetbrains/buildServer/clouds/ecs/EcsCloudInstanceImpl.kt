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

package jetbrains.buildServer.clouds.ecs

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.agent.Constants
import jetbrains.buildServer.clouds.CloudErrorInfo
import jetbrains.buildServer.clouds.CloudImage
import jetbrains.buildServer.clouds.InstanceStatus
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsTask
import jetbrains.buildServer.serverSide.AgentDescription
import java.util.*

class EcsCloudInstanceImpl(private val instanceId: String, val cloudImage: EcsCloudImage, val ecsTask: EcsTask, val apiConnector: EcsApiConnector) : EcsCloudInstance {
    private val LOG = Logger.getInstance(EcsCloudInstanceImpl::class.java.getName())
    private var myCurrentError: CloudErrorInfo? = null
    private var myTask: EcsTask = ecsTask

    override val taskArn: String
        get() = ecsTask.arn

    override fun getStatus(): InstanceStatus {
        val lastStatus = myTask.lastStatus
        when (myTask.desiredStatus) {
            "RUNNING" -> {
                when(lastStatus){
                    "PENDING" -> return InstanceStatus.STARTING
                    "RUNNING" -> return InstanceStatus.RUNNING
                    else -> return InstanceStatus.RUNNING
                }
            }
            "STOPPED" -> {
                when(lastStatus){
                    "RUNNING" -> return InstanceStatus.STOPPING
                    "PENDING" -> return InstanceStatus.STOPPED
                    "STOPPED" -> return InstanceStatus.STOPPED
                    else -> return InstanceStatus.STOPPED
                }
            }
            else -> return InstanceStatus.UNKNOWN
        }
    }

    override fun getInstanceId(): String {
        return instanceId
    }

    override fun getName(): String {
        return ecsTask.id
    }

    override fun getStartedTime(): Date {
        val startedAt = ecsTask.startedAt
        when {
            startedAt != null -> return startedAt
            else -> return ecsTask.cratedAt
        }
    }

    override fun getNetworkIdentity(): String? {
        //TODO: provide identity
        return null
    }

    override fun getImage(): CloudImage {
        return cloudImage
    }

    override fun getImageId(): String {
        return cloudImage.id
    }

    override fun getErrorInfo(): CloudErrorInfo? {
        return myCurrentError
    }

    override fun containsAgent(agent: AgentDescription): Boolean {
        if (agent.configurationParameters[REQUIRED_PROFILE_ID_CONFIG_PARAM] == null)
            return false

        return instanceId == agent.configurationParameters[Constants.ENV_PREFIX + INSTANCE_ID_ECS_ENV]
    }

    override fun terminate() {
        try{
            apiConnector.stopTask(ecsTask.arn, ecsTask.clusterArn, "Terminated by TeamCity server")
            myCurrentError = null
        } catch (ex:Exception){
            val msg = "Failed to stop cloud instance with id $instanceId"
            LOG.warnAndDebugDetails(msg, ex)
            myCurrentError = CloudErrorInfo(msg)
        }
        cloudImage.populateInstances()
    }

    override fun update(task: EcsTask) {
        LOG.debug("Updating task '${task.id}:${task.arn}', status: ${task.lastStatus} -> ${task.desiredStatus}")
        myTask = task
    }
}
