package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.agent.Constants
import jetbrains.buildServer.clouds.*
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnectorImpl
import jetbrains.buildServer.clouds.ecs.web.EDIT_ECS_HTML
import jetbrains.buildServer.serverSide.AgentDescription
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.serverSide.ServerSettings
import jetbrains.buildServer.util.amazon.AWSCommonParams
import jetbrains.buildServer.web.openapi.PluginDescriptor
import java.util.*

fun startedByTeamCity(serverUUID: String?): String {
    val string = serverUUID.toString()
    if(string.length > 36)
        return string.substring(0, 36)
    else
        return string
}

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 05.07.17.
 */
class EcsCloudClientFactory(cloudRegister: CloudRegistrar,
                            pluginDescriptor: PluginDescriptor,
                            private val serverSettings: ServerSettings,
                            private val cache: EcsDataCache) : CloudClientFactory {
    val editUrl = pluginDescriptor.getPluginResourcesPath(EDIT_ECS_HTML)

    init {
        cloudRegister.registerCloudFactory(this)
    }

    override fun getCloudCode(): String {
        return "awsecs"
    }

    override fun getDisplayName(): String {
        return "Amazon EC2 Container Service"
    }

    override fun getEditProfileUrl(): String? {
        return editUrl
    }

    override fun canBeAgentOfType(description: AgentDescription): Boolean {
        val map = description.availableParameters
        return map.containsKey(Constants.ENV_PREFIX + SERVER_UUID_ECS_ENV) &&
                map.containsKey(Constants.ENV_PREFIX + PROFILE_ID_ECS_ENV) &&
                map.containsKey(Constants.ENV_PREFIX + IMAGE_ID_ECS_ENV) &&
                map.containsKey(Constants.ENV_PREFIX + INSTANCE_ID_ECS_ENV)
    }

    override fun createNewClient(state: CloudState, params: CloudClientParameters): CloudClientEx {
        val ecsParams = params.toEcsParams()
        val apiConnector = EcsApiConnectorImpl(ecsParams.awsCredentials, ecsParams.region)
        val serverUUID = serverSettings.serverUUID!!
        val images = ecsParams.imagesData.map{
            val image = it.toImage(apiConnector, cache, serverUUID)
            image.populateInstances()
            image
        }
        return EcsCloudClient(images, apiConnector, cache, ecsParams, serverUUID, state.profileId)
    }

    override fun getInitialParameterValues(): MutableMap<String, String> {
        val result = HashMap<String, String>()
        result.putAll(AWSCommonParams.getDefaults(serverSettings.getServerUUID()))
        return result
    }

    override fun getPropertiesProcessor(): PropertiesProcessor {
        return object: PropertiesProcessor{
            override fun process(properties: MutableMap<String, String>?): MutableCollection<InvalidProperty> {
                val invalids = ArrayList<InvalidProperty>()
                for (e in AWSCommonParams.validate(properties!!, false)) {
                    invalids.add(InvalidProperty(e.key, e.value))
                }
                return invalids
            }
        }
    }
}