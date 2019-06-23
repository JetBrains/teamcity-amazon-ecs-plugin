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
    private val REGEX = Regex("arn:aws:ecs:[^:]+:\\d+:task\\/(\\w+\\/\\w+)")

    init {
        agentEvents.addListener(object : AgentLifeCycleAdapter() {
            override fun afterAgentConfigurationLoaded(agent: BuildAgent) {
                super.afterAgentConfigurationLoaded(agent)
                appendEcsSpecificConfiguration()
            }
        })
    }

    private fun readMetaDataV2(): String?{
        val client = HttpUtil.createHttpClient(5)
        val uri = "http://169.254.170.2/v2/metadata"
        val get = GetMethod(uri)
        client.executeMethod(get)
        if (get.statusCode != HttpStatus.SC_OK) {
            throw RuntimeException("Server returned [" + get.statusCode + "] " + get.statusText + " for " + uri)
        }
        val response = get.responseBodyAsString
        val obj = JSONObject(response)
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
        if (environment[ECS_CONTAINER_METADATA_URI] != null){
            val data = readMetaDataV2()
            if (data != null) {
                agentConfigurationEx.name = data
            }
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

fun main(args: Array<String>) {
    val obj = JSONObject(File("/Users/sergeypak/projects/Plugins/teamcity-amazon-ecs-plugin/data.json").readText())
    val taskArn = obj.getString("TaskARN")
    val REGEX = Regex("arn:aws:ecs:[^:]+:\\d+:task\\/(\\w+\\/\\w+)")
    val find = REGEX.find(taskArn)
    println(find?.groups?.get(1)?.value)

}