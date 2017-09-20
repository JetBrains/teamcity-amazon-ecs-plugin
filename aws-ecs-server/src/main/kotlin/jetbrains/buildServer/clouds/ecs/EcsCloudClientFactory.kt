package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.*
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnectorImpl
import jetbrains.buildServer.clouds.ecs.web.EDIT_ECS_HTML
import jetbrains.buildServer.serverSide.AgentDescription
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.web.openapi.PluginDescriptor
import java.util.*

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 05.07.17.
 */
class EcsCloudClientFactory(cloudRegister: CloudRegistrar, pluginDescriptor: PluginDescriptor) : CloudClientFactory {
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createNewClient(state: CloudState, params: CloudClientParameters): CloudClientEx {
        val ecsParams = params.toEcsParams()
        val apiConnector = EcsApiConnectorImpl(ecsParams)
        val images = ecsParams.imagesData.map{
            val image = it.toImage(apiConnector)
            image.populateInstances()
            image
        }
        return EcsCloudClient(images, apiConnector, ecsParams)
    }

    override fun getInitialParameterValues(): MutableMap<String, String> {
        return Collections.emptyMap()
    }

    override fun getPropertiesProcessor(): PropertiesProcessor {
        return PropertiesProcessor {
            props -> emptyList()
        }
    }
}