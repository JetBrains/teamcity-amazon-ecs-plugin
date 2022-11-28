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

import com.amazonaws.services.ecs.model.LaunchType
import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.clouds.CloudErrorInfo
import jetbrains.buildServer.clouds.CloudException
import jetbrains.buildServer.clouds.CloudInstance
import jetbrains.buildServer.clouds.CloudInstanceUserData
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsTask
import jetbrains.buildServer.serverSide.TeamCityProperties
import jetbrains.buildServer.util.FileUtil
import jetbrains.buildServer.util.StringUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


class EcsCloudImageImpl(private val imageData: EcsCloudImageData,
                        private val apiConnector: EcsApiConnector,
                        private val serverUUID: String,
                        idxStorage: File,
                        private val profileId: String) : EcsCloudImage {

    private val LOG = Logger.getInstance(EcsCloudImageImpl::class.java.getName())
    private val ERROR_INSTANCES_TIMEOUT: Long = 60*1000

    private val idxFile = File(idxStorage, imageName4File() + ".idx")
    private val idxCounter = AtomicInteger(0)
    private val idxTouched = AtomicBoolean(false)
    private val idxMutex = Mutex()
    private val counterContext = newSingleThreadContext("IdxContext")
    private val errorInstances= ConcurrentHashMap<String, Pair<EcsCloudInstance, Long>>()

    private val muteTime = AtomicLong(0)


    init{
        try {
            if (!idxFile.exists()) {
                idxCounter.set(1)
                idxTouched.set(true)
                storeIdx()
            } else {
                runBlocking {
                    idxMutex.withLock {
                        idxCounter.set(Integer.parseInt(FileUtil.readText(idxFile)))
                    }
                }
            }
        } catch (ex: Exception) {
            LOG.warnAndDebugDetails("Unable to process idx file '${idxFile.absolutePath}'. Will reset the index for ${imageData.taskDefinition}", ex)
            idxCounter.set(1)
        }

        GlobalScope.async{
            while (true) {
                try {
                    storeIdx()
                    expireErrorInstances()
                    delay(1000)
                } catch (ex: Exception){
                    LOG.warnAndDebugDetails("An error occurred during processing of periodic tasks", ex)
                }
            }
        }
    }

    override fun canStartNewInstance(): Boolean {
        if (System.currentTimeMillis() < muteTime.get())
            return false
        if(instanceLimit in 1..runningInstanceCount) return false
        val monitoringPeriod = TeamCityProperties.getInteger(ECS_METRICS_MONITORING_PERIOD, 1)
        return cpuReservalionLimit <= 0 || apiConnector.getMaxCPUReservation(cluster, monitoringPeriod) < cpuReservalionLimit
    }


    private val myIdToInstanceMap = ConcurrentHashMap<String, EcsCloudInstance>()
    private var myCurrentError: CloudErrorInfo? = null

    private val instanceLimit: Int
        get() = imageData.instanceLimit

    private val cpuReservalionLimit: Int
        get() = imageData.cpuReservalionLimit

    private val cluster: String?
        get() = imageData.cluster

    private val taskGroup: String?
        get() = imageData.taskGroup

    private val subnets: Collection<String>
        get() {
            val rawSubnetsString = imageData.subnets?.trim()
            return if(rawSubnetsString.isNullOrEmpty()) emptyList() else rawSubnetsString!!.lines()
        }

    private val securityGroups: Collection<String>
        get() {
            val rawSecurityGroupsString = imageData.securityGroups?.trim()
            return if(rawSecurityGroupsString.isNullOrEmpty()) emptyList() else rawSecurityGroupsString!!.lines()
        }

    private val assignPublicIp: Boolean
        get() = imageData.assignPublicIp

    private val launchType: LaunchType?
        get() {
            return try {
                LaunchType.fromValue(imageData.launchType)
            } catch (ex:Exception)  {
                null
            }
        }

    private val fargatePlatformVersion: String?
        get() {
            if (launchType != LaunchType.EC2){
                return imageData.fargatePlatformVersion
            } else {
                return null
            }
        }

    private val taskDefinition: String
        get() = imageData.taskDefinition

    override val runningInstanceCount: Int
        get() = myIdToInstanceMap.filterValues { instance -> instance.status.isStartingOrStarted }.size

    override fun getAgentPoolId(): Int? {
        return imageData.agentPoolId
    }

    override fun getName(): String {
        return taskDefinition
    }

    override fun getId(): String {
        return imageData.id
    }

    override fun getInstances(): MutableCollection<out CloudInstance> {
        return myIdToInstanceMap.values
    }

    override fun getErrorInfo(): CloudErrorInfo? {
        return myCurrentError
    }

    override fun findInstanceById(id: String): CloudInstance? {
        return myIdToInstanceMap[id]
    }

    override fun populateInstances() {
        LOG.debug("Populating instances for $name")
        try {
            val startedBy = startedByTeamCity(serverUUID)

            val runningTasks = apiConnector.listRunningTasks(cluster, startedBy).mapNotNull { taskArn -> apiConnector.describeTask(taskArn, cluster) }
            val stoppedTasks = apiConnector.listStoppedTasks(cluster, startedBy).mapNotNull { taskArn -> apiConnector.describeTask(taskArn, cluster) }
            LOG.debug("Will process ${runningTasks.size} running and ${stoppedTasks.size} stopped tasks")

            synchronized(myIdToInstanceMap) {
                val keySet = HashSet(myIdToInstanceMap.keys)
                val newTasks = ArrayList<EcsTask>()
                for (task in runningTasks.union(stoppedTasks)) {
                    val taskProfileId = task.getOverridenContainerEnv(PROFILE_ID_ECS_ENV)
                    val taskImageId = task.getOverridenContainerEnv(IMAGE_ID_ECS_ENV)
                    if(profileId == taskProfileId && taskImageId == id){
                        val instanceId = task.getOverridenContainerEnv(INSTANCE_ID_ECS_ENV)
                        if(instanceId == null) {
                            LOG.warn("Can't resolve cloud instance id of ecs task ${task.arn}")
                            continue
                        }
                        if (keySet.remove(instanceId)) {
                            val instance = myIdToInstanceMap[instanceId]
                            if (instance == null) {
                                LOG.warn("Unable to find instance with id '$instanceId'. Was it removed?")
                                continue
                            }
                            instance.update(task)
                        } else {
                            newTasks.add(task)
                        }
                    }
                }
                //remove absent instances
                keySet.forEach {
                    LOG.info("Instance '$it' is no longer available")
                    myIdToInstanceMap.remove(it)
                }
                newTasks.forEach{
                    val instanceId = it.getOverridenContainerEnv(INSTANCE_ID_ECS_ENV)
                    if (instanceId != null) {
                        LOG.info("Found new instance '$instanceId'")
                        myIdToInstanceMap[instanceId] = EcsCloudInstanceImpl(instanceId, this, it, apiConnector)
                    } else {
                        LOG.info("Found no instance id for task with arn '${it.arn}'")
                    }
                }
                myCurrentError = null
            }
        } catch (ex: Throwable) {
            val msg = "Unable to populate instances for ${imageData.id}"
            LOG.warnAndDebugDetails(msg, ex)
            myCurrentError = CloudErrorInfo(msg, ex.message.toString(), ex)
        }
    }


    @Synchronized
    override fun startNewInstance(tag: CloudInstanceUserData): EcsCloudInstance {
        val instanceId = generateNewInstanceId()
        val startingInstance = StartingEcsCloudInstance(instanceId, this)
        myIdToInstanceMap[instanceId] = startingInstance
        LOG.debug("attempting to start new ECS instance with generated instanceId: $instanceId")
        try {
            val taskDefinition = apiConnector.describeTaskDefinition(taskDefinition) ?: throw CloudException("""Task definition $taskDefinition is missing""")

            val additionalEnvironment = HashMap<String, String>()
            additionalEnvironment[SERVER_UUID_ECS_ENV] = serverUUID
            additionalEnvironment[SERVER_URL_ECS_ENV] = tag.serverAddress
            additionalEnvironment[OFFICIAL_IMAGE_SERVER_URL_ECS_ENV] = tag.serverAddress
            additionalEnvironment[PROFILE_ID_ECS_ENV] = tag.profileId
            additionalEnvironment[IMAGE_ID_ECS_ENV] = id
            additionalEnvironment[INSTANCE_ID_ECS_ENV] = instanceId
            additionalEnvironment[AGENT_NAME_ECS_ENV] = generateAgentName(instanceId)

            for (pair in tag.customAgentConfigurationParameters){
                additionalEnvironment[TEAMCITY_ECS_PROVIDED_PREFIX + pair.key] = pair.value
            }

            val tasks = apiConnector.runTask(launchType, taskDefinition, cluster, taskGroup, subnets, securityGroups,
                    assignPublicIp, additionalEnvironment, startedByTeamCity(serverUUID), fargatePlatformVersion)
            LOG.info("Started ECS instance ${tasks[0].id}, generatedInstanceId: $instanceId")
            val startedInstance = EcsCloudInstanceImpl(instanceId, this, tasks[0], apiConnector)
            myIdToInstanceMap[instanceId] = startedInstance
            if (startingInstance.terminateRequested){
                startedInstance.terminate()
            }
            return startedInstance
        } catch (ex: Throwable){
            val errInstance = BrokenEcsCloudInstance(instanceId, this, CloudErrorInfo(ex.message.toString(), ex.message.toString(), ex))
            myIdToInstanceMap[instanceId] = errInstance
            errorInstances[instanceId] = Pair(errInstance, System.currentTimeMillis() + ERROR_INSTANCES_TIMEOUT)
            muteTime.set(System.currentTimeMillis() + ERROR_INSTANCES_TIMEOUT)
            return errInstance
        }
    }

    override fun generateAgentName(instanceId: String): String {
        return imageData.agentNamePrefix + instanceId
    }

    private fun generateNewInstanceId(): String {
        lateinit var retval : String
        do{
            retval = String.format("${imageData.taskDefinition}-${idxCounter.getAndIncrement()}")
        } while (myIdToInstanceMap.containsKey(retval))
        LOG.info("Will create a new instance with name $retval")
        return retval
    }

    private fun imageName4File():String {
        return StringUtil.replaceNonAlphaNumericChars(imageData.taskDefinition, '_')
    }

    private fun storeIdx() = runBlocking {
        if (idxTouched.compareAndSet(true, false)){
            idxMutex.withLock {
                FileUtil.writeViaTmpFile(idxFile, ByteArrayInputStream(idxCounter.get().toString().toByteArray()),
                        FileUtil.IOAction.DO_NOTHING)
            }
        }
    }

    private fun expireErrorInstances() =  runBlocking {
        errorInstances.forEach{
            if (it.value.second < System.currentTimeMillis()) {
                errorInstances.remove(it.key)
            }
        }
    }
}
