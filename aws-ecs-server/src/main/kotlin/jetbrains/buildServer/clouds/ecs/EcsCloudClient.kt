package jetbrains.buildServer.clouds.ecs

import com.google.common.collect.Maps
import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.clouds.*
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.serverSide.AgentDescription
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class EcsCloudClient(images: List<EcsCloudImage>,
                     val apiConnector: EcsApiConnector,
                     val ecsClientParams: EcsCloudClientParameters,
                     private val serverUuid: String,
                     private val cloudProfileId: String) : CloudClientEx {
    private val LOG = Logger.getInstance(EcsCloudClient::class.java.getName())

    private var myCurrentlyRunningInstancesCount: Int = 0
    private var myCurrentError: CloudErrorInfo? = null
    private var myImageNameToImageMap: ConcurrentHashMap<String, EcsCloudImage>
    private var myImageIdToImageMap: ConcurrentHashMap<String, EcsCloudImage>

    init {
        myImageNameToImageMap = ConcurrentHashMap(Maps.uniqueIndex(images, { it?.name }))
        myImageIdToImageMap = ConcurrentHashMap(Maps.uniqueIndex(images, { it?.id }))
    }


    override fun isInitialized(): Boolean {
        //TODO: wait while all images populate list of their instances
        return true
    }

    override fun dispose() {
        LOG.debug("EcsCloudClient disposed")
    }

    override fun getErrorInfo(): CloudErrorInfo? {
        return myCurrentError
    }

    override fun canStartNewInstance(image: CloudImage): Boolean {
        val kubeCloudImage = image as EcsCloudImage
        val kubeCloudImageId = kubeCloudImage.getId()
        if (!myImageIdToImageMap.containsKey(kubeCloudImageId)) {
            LOG.debug("Can't start instance of unknown cloud image with id " + kubeCloudImageId)
            return false
        }
        val profileInstanceLimit = ecsClientParams.instanceLimit
        if (profileInstanceLimit > 0 && myCurrentlyRunningInstancesCount >= profileInstanceLimit)
            return false

        val imageLimit = kubeCloudImage.instanceLimit
        return imageLimit <= 0 || kubeCloudImage.instanceCount < imageLimit
    }

    override fun startNewInstance(image: CloudImage, tag: CloudInstanceUserData): CloudInstance {
        val ecsImage = image as EcsCloudImage
        val tasks = apiConnector.runTask(ecsImage.taskDefinition, ecsImage.cluster, ecsImage.taskGroup)
        val newInstance = EcsCloudInstanceImpl(ecsImage, tasks[0], apiConnector)
        ecsImage.addInstance(newInstance)
        myCurrentError = null
        myCurrentlyRunningInstancesCount++
        return newInstance
    }

    override fun terminateInstance(instance: CloudInstance) {
        val kubeCloudInstance = instance as EcsCloudInstance
        kubeCloudInstance.terminate()
        myCurrentlyRunningInstancesCount--
    }

    override fun restartInstance(instance: CloudInstance) {
        throw UnsupportedOperationException("Restart not implemented")
    }

    override fun generateAgentName(agent: AgentDescription): String? {
        return agent.getAvailableParameters().get(INSTANCE_NAME_AGENT_PROP)
    }

    override fun getImages(): MutableCollection<out CloudImage> {
        return Collections.unmodifiableCollection(myImageNameToImageMap.values)
    }

    override fun findImageById(imageId: String): CloudImage? {
        return myImageIdToImageMap.get(imageId)
    }

    override fun findInstanceByAgent(agent: AgentDescription): CloudInstance? {
        val agentParameters = agent.getAvailableParameters()

        if (serverUuid != agentParameters.get(SERVER_UUID_AGENT_PROP) || cloudProfileId != agentParameters.get(PROFILE_ID_AGENT_PROP))
            return null

        val imageName = agentParameters.get(IMAGE_NAME_AGENT_PROP)
        val instanceName = agentParameters.get(INSTANCE_NAME_AGENT_PROP)
        if (imageName != null && instanceName != null) {
            val cloudImage = myImageNameToImageMap[imageName]
            if (cloudImage != null) {
                return cloudImage.findInstanceById(instanceName)
            }
        }
        return null
    }
}