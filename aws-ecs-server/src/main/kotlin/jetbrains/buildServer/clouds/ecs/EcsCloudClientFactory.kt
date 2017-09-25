package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.*
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnectorImpl
import jetbrains.buildServer.clouds.ecs.web.EDIT_ECS_HTML
import jetbrains.buildServer.serverSide.AgentDescription
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.serverSide.ServerSettings
import jetbrains.buildServer.util.amazon.AWSCommonParams
import jetbrains.buildServer.web.openapi.PluginDescriptor
import java.util.*

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 05.07.17.
 */
class EcsCloudClientFactory(cloudRegister: CloudRegistrar,
                            pluginDescriptor: PluginDescriptor,
                            private val serverSettings: ServerSettings) : CloudClientFactory {
    val editUrl = pluginDescriptor.getPluginResourcesPath(EDIT_ECS_HTML)

    init {
        cloudRegister.registerCloudFactory(this)
    }

    override fun getCloudCode(): String {
        return "aws-ecs"
    }

    override fun getDisplayName(): String {
        return "Amazon EC2 Container Service"
    }

    override fun getEditProfileUrl(): String? {
        return editUrl
    }

    override fun canBeAgentOfType(description: AgentDescription): Boolean {
        val map = description.getAvailableParameters()
        return map.containsKey(SERVER_UUID_AGENT_PROP) &&
                map.containsKey(PROFILE_ID_AGENT_PROP) &&
                map.containsKey(IMAGE_NAME_AGENT_PROP) &&
                map.containsKey(INSTANCE_NAME_AGENT_PROP)
    }

    override fun createNewClient(state: CloudState, params: CloudClientParameters): CloudClientEx {
        val ecsParams = params.toEcsParams()
        val apiConnector = EcsApiConnectorImpl(ecsParams)
        val images = ecsParams.imagesData.map{
            val image = it.toImage(apiConnector)
            image.populateInstances()
            image
        }
        return EcsCloudClient(images, apiConnector, ecsParams, serverSettings.getServerUUID()!!, state.getProfileId())
    }

    override fun getInitialParameterValues(): MutableMap<String, String> {
        val result = HashMap<String, String>()
        result.putAll(AWSCommonParams.getDefaults(serverSettings.getServerUUID()))
        return result
    }

    override fun getPropertiesProcessor(): PropertiesProcessor {
        return PropertiesProcessor {
            emptyList()
        }
    }
}