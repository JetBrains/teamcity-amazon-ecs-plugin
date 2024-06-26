

package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.agent.AgentLifeCycleAdapter
import jetbrains.buildServer.agent.AgentLifeCycleListener
import jetbrains.buildServer.agent.BuildAgent
import jetbrains.buildServer.agent.BuildAgentConfigurationEx
import jetbrains.buildServer.http.HttpUtil
import jetbrains.buildServer.util.EventDispatcher
import jetbrains.buildServer.util.StringUtil
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import org.json.JSONObject
import java.io.File
import java.lang.RuntimeException

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 20.09.17.
 */
class EcsAgentConfigurationProvider(agentEvents: EventDispatcher<AgentLifeCycleListener>,
                                    private val agentConfigurationEx: BuildAgentConfigurationEx) {
    private val REGEX = Regex("arn:aws:ecs:[^:]+:\\d+:task\\/(.+)")

    init {
        agentEvents.addListener(object : AgentLifeCycleAdapter() {
            override fun afterAgentConfigurationLoaded(agent: BuildAgent) {
                super.afterAgentConfigurationLoaded(agent)
                appendEcsSpecificConfiguration()
            }
        })
    }

    private fun readMetaDatFromUrl(metaDataUrl: String): String?{
        val client = HttpUtil.createHttpClient(5)
//        val uri = "http://169.254.170.2/v2/metadata"
        val get = GetMethod(metaDataUrl + "/task")
        try {
            client.executeMethod(get)
            if (get.statusCode != HttpStatus.SC_OK) {
                throw RuntimeException("Server returned [" + get.statusCode + "] " + get.statusText + " for " + metaDataUrl)
            }
            val response = get.responseBodyAsString
            val obj = JSONObject(response)
            val taskArn = obj.getString("TaskARN")
            val find = REGEX.find(taskArn)
            return find?.groups?.get(1)?.value
        } finally {
            get.releaseConnection()
        }
    }

    private fun readMetaDataFile(metadataFilePath: String): String? {
        val obj = JSONObject(File(metadataFilePath).readText())
        val taskArn = obj.getString("TaskARN")
        val find = REGEX.find(taskArn)
        return find?.groups?.get(1)?.value
    }

    private fun appendEcsSpecificConfiguration() {
        val environment = System.getenv()
        val providedServerUrl = environment[SERVER_URL_ECS_ENV]
        if (!StringUtil.isEmpty(providedServerUrl)) agentConfigurationEx.serverUrl = providedServerUrl

        val profileId = environment[PROFILE_ID_ECS_ENV]
        if (!StringUtil.isEmpty(profileId)) agentConfigurationEx.addConfigurationParameter(REQUIRED_PROFILE_ID_CONFIG_PARAM, profileId!!)
        if (environment[AGENT_NAME_ECS_ENV] != null) {
            agentConfigurationEx.name = environment[AGENT_NAME_ECS_ENV]
        } else if (environment[ECS_CONTAINER_METADATA_URI] != null){
            val data = readMetaDatFromUrl(environment[ECS_CONTAINER_METADATA_URI]!!)
            if (data != null) {
                agentConfigurationEx.name = data
            }
        } else if (environment[ECS_CONTAINER_METADATA_FILE] != null) {
            val data = readMetaDataFile(environment[ECS_CONTAINER_METADATA_FILE]!!)
            if (data != null) {
                agentConfigurationEx.name = data
            }
        }

        if (environment[STARTING_INSTANCE_ID_ECS_ENV] != null) {
            agentConfigurationEx.addConfigurationParameter(STARTING_INSTANCE_ID_CONFIG_PARAM, environment[STARTING_INSTANCE_ID_ECS_ENV].toString());
        }

        environment.entries.forEach { entry ->
            val key = entry.key
            val value = entry.value
            if (key.startsWith(TEAMCITY_ECS_PROVIDED_PREFIX)){
                agentConfigurationEx.addConfigurationParameter(key.removePrefix(TEAMCITY_ECS_PROVIDED_PREFIX), value)
            }
        }
    }
}