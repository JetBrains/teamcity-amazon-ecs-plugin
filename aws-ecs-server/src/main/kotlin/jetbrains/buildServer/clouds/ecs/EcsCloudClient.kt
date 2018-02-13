package jetbrains.buildServer.clouds.ecs

import com.google.common.collect.Maps
import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.agent.Constants
import jetbrains.buildServer.clouds.*
import jetbrains.buildServer.serverSide.AgentDescription
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class EcsCloudClient(images: List<EcsCloudImage>,
                     private val updater: EcsInstancesUpdater,
                     private val ecsClientParams: EcsCloudClientParameters,
                     private val serverUuid: String,
                     private val cloudProfileId: String) : CloudClientEx {
    private val LOG = Logger.getInstance(EcsCloudClient::class.java.getName())

    private var myCurrentError: CloudErrorInfo? = null
    private var myImageIdToImageMap: ConcurrentHashMap<String, EcsCloudImage> = ConcurrentHashMap(Maps.uniqueIndex(images, { it?.id }))

    init {
        updater.registerClient(this)
    }

    override fun isInitialized(): Boolean {
        //TODO: wait while all images populate list of their instances
        return true
    }

    override fun dispose() {
        updater.unregisterClient(this)
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

        if (ecsClientParams.instanceLimit in 1..images.sumBy { image.runningInstanceCount })
            return false

        return kubeCloudImage.canStartNewInstance()
    }

    override fun startNewInstance(image: CloudImage, tag: CloudInstanceUserData): CloudInstance {
        try{
            val newInstance = (image as EcsCloudImage).startNewInstance(tag)
            myCurrentError = null
            return newInstance
        } catch (ex: Exception){
            LOG.debug("Failed to start cloud instance", ex)
            myCurrentError = CloudErrorInfo("Failed to start cloud instance", ex.localizedMessage, ex)
            throw ex
        }
    }

    override fun terminateInstance(instance: CloudInstance) {
        val kubeCloudInstance = instance as EcsCloudInstance
        kubeCloudInstance.terminate()
    }

    override fun restartInstance(instance: CloudInstance) {
        throw UnsupportedOperationException("Restart not implemented")
    }

    override fun generateAgentName(agent: AgentDescription): String? {
        val instanceId = agent.configurationParameters.get(Constants.ENV_PREFIX + INSTANCE_ID_ECS_ENV)
        val imageId = agent.configurationParameters.get(Constants.ENV_PREFIX + IMAGE_ID_ECS_ENV)
        val image = myImageIdToImageMap.get(imageId)
        if(instanceId.isNullOrEmpty() || image == null) return null;
        return image.generateAgentName(instanceId!!);
    }

    override fun getImages(): MutableCollection<out CloudImage> {
        return Collections.unmodifiableCollection(myImageIdToImageMap.values)
    }

    override fun findImageById(imageId: String): CloudImage? {
        return myImageIdToImageMap.get(imageId)
    }

    override fun findInstanceByAgent(agent: AgentDescription): CloudInstance? {
        val agentParameters = agent.getAvailableParameters()

        if (serverUuid != agentParameters.get(Constants.ENV_PREFIX + SERVER_UUID_ECS_ENV) || cloudProfileId != agentParameters.get(Constants.ENV_PREFIX + PROFILE_ID_ECS_ENV))
            return null

        val imageId = agentParameters.get(Constants.ENV_PREFIX + IMAGE_ID_ECS_ENV)
        val instanceId = agentParameters.get(Constants.ENV_PREFIX + INSTANCE_ID_ECS_ENV)
        if (imageId != null && instanceId != null) {
            val cloudImage = myImageIdToImageMap[imageId]
            if (cloudImage != null) {
                return cloudImage.findInstanceById(instanceId)
            }
        }
        return null
    }
}