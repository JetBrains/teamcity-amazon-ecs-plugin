

package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.agent.Constants
import jetbrains.buildServer.clouds.*
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnectorImpl
import jetbrains.buildServer.clouds.ecs.web.EDIT_ECS_HTML
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.util.amazon.AWSCommonParams
import jetbrains.buildServer.web.openapi.PluginDescriptor
import java.io.File
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
                            private val pluginDescriptor: PluginDescriptor,
                            serverPaths: ServerPaths,
                            private val serverSettings: ServerSettings,
                            private val instanceUpdater: EcsInstancesUpdater) : CloudClientFactory {
    private val editUrl = pluginDescriptor.getPluginResourcesPath(EDIT_ECS_HTML)
    private val idxStorage = File(serverPaths.pluginDataDirectory, "ecsCloudIdx")

    init {
        cloudRegister.registerCloudFactory(this)
        if (!idxStorage.exists()){
            idxStorage.mkdirs()
        }
    }

    override fun getCloudCode(): String {
        return "awsecs"
    }

    override fun getDisplayName(): String {
        return "Amazon Elastic Container Service"
    }

    override fun getEditProfileUrl(): String? {
        return editUrl
    }

    override fun getTypeDescription(): String = """
        Agents are linux containers running in AWS ECS cluster. Provides a high availability of agents. Doesn't support Docker containers nor Docker compose
    """.trimIndent()

    override fun getProfileIconUrl(): String = pluginDescriptor.getPluginResourcesPath("ecs.svg")

    override fun canBeAgentOfType(description: AgentDescription): Boolean {
        val map = description.availableParameters
        return map.containsKey(Constants.ENV_PREFIX + SERVER_UUID_ECS_ENV) &&
                map.containsKey(Constants.ENV_PREFIX + PROFILE_ID_ECS_ENV) &&
                map.containsKey(Constants.ENV_PREFIX + IMAGE_ID_ECS_ENV) &&
                map.containsKey(Constants.ENV_PREFIX + INSTANCE_ID_ECS_ENV)
    }

    override fun createNewClient(state: CloudState, params: CloudClientParameters): CloudClientEx {
        val ecsParams = params.toEcsParams()
        val apiConnector = EcsApiConnectorImpl(ecsParams.awsCredentialsProvider, ecsParams.region)
        val serverUUID = serverSettings.serverUUID!!
        val images = ecsParams.imagesData.map{
            val image = it.toImage(apiConnector, serverUUID, idxStorage, state.profileId)
            image.populateInstances()
            image
        }
        return EcsCloudClient(images, instanceUpdater, ecsParams, serverUUID, idxStorage, state.profileId)
    }

    override fun getInitialParameterValues(): MutableMap<String, String> {
        val result = HashMap<String, String>()
        result.putAll(AWSCommonParams.getDefaults(serverSettings.serverUUID))
        return result
    }

    override fun getPropertiesProcessor(): PropertiesProcessor {
        return PropertiesProcessor { properties ->
            val invalids = ArrayList<InvalidProperty>()
            for (e in AWSCommonParams.validate(properties!!, false)) {
                invalids.add(InvalidProperty(e.key, e.value))
            }
            invalids
        }
    }
}