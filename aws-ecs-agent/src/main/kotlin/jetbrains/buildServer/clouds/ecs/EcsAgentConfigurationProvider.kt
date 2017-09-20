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
        val env = System.getenv()

        val providedAgentName = env[AGENT_NAME_ECS_ENV]
        if (!StringUtil.isEmpty(providedAgentName)) agentConfigurationEx.name = providedAgentName
        val providedServerUrl = env[SERVER_URL_ECS_ENV]
        if (!StringUtil.isEmpty(providedServerUrl)) agentConfigurationEx.serverUrl = providedServerUrl

        with(agentConfigurationEx) {
            addConfigurationParameter(SERVER_UUID_AGENT_PROP, env[SERVER_UUID_ECS_ENV]!!)
            addConfigurationParameter(PROFILE_ID_AGENT_PROP, env[PROFILE_ID_ECS_ENV]!!)
            addConfigurationParameter(IMAGE_NAME_AGENT_PROP, env[IMAGE_NAME_ECS_ENV]!!)
            addConfigurationParameter(INSTANCE_NAME_AGENT_PROP, env[INSTANCE_NAME_ECS_ENV]!!)
        }
    }
}