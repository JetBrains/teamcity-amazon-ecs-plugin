package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.agent.AgentLifeCycleAdapter
import jetbrains.buildServer.agent.AgentLifeCycleListener
import jetbrains.buildServer.agent.BuildAgent
import jetbrains.buildServer.agent.BuildAgentConfigurationEx
import jetbrains.buildServer.util.EventDispatcher
import jetbrains.buildServer.util.StringUtil

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 20.09.17.
 */
class EcsAgentConfigurationProvider(agentEvents: EventDispatcher<AgentLifeCycleListener>,
                                    private val agentConfigurationEx: BuildAgentConfigurationEx) {
    init {
        agentEvents.addListener(object : AgentLifeCycleAdapter() {
            override fun afterAgentConfigurationLoaded(agent: BuildAgent) {
                super.afterAgentConfigurationLoaded(agent)
                appendEcsSpecificConfiguration()
            }
        })
    }

    private fun appendEcsSpecificConfiguration() {
        val environment = System.getenv()
        val providedServerUrl = environment[SERVER_URL_ECS_ENV]
        if (!StringUtil.isEmpty(providedServerUrl)) agentConfigurationEx.serverUrl = providedServerUrl

        environment.entries.forEach { entry ->
            val key = entry.key
            val value = entry.value
            if (key.startsWith(TEAMCITY_ECS_PROVIDED_PREFIX)){
                agentConfigurationEx.addConfigurationParameter(key.removePrefix(TEAMCITY_ECS_PROVIDED_PREFIX), value)
            }
        }
    }
}