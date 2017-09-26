package jetbrains.buildServer.clouds.ecs.web

import jetbrains.buildServer.BuildProject
import jetbrains.buildServer.controllers.BaseFormXmlController
import jetbrains.buildServer.serverSide.agentPools.AgentPool
import jetbrains.buildServer.serverSide.agentPools.AgentPoolManager
import jetbrains.buildServer.serverSide.agentPools.AgentPoolUtil
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.jdom.Element
import org.springframework.web.servlet.ModelAndView
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

const val EDIT_ECS_HTML = "editEcs.html"

class EcsProfileEditController(val pluginDescriptor: PluginDescriptor,
                               val agentPoolManager: AgentPoolManager,
                               web: WebControllerManager) : BaseFormXmlController() {
    init {
        web.registerController(pluginDescriptor.getPluginResourcesPath(EDIT_ECS_HTML), this)
    }

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse, xmlResponse: Element) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun doGet(request: HttpServletRequest, response: HttpServletResponse): ModelAndView {
        val modelAndView = ModelAndView(pluginDescriptor.getPluginResourcesPath("editProfile.jsp"))
        val projectId = request.getParameter("projectId")
        val pools = ArrayList<AgentPool>()
        if (BuildProject.ROOT_PROJECT_ID != projectId) {
            pools.add(AgentPoolUtil.DUMMY_PROJECT_POOL)
        }
        pools.addAll(agentPoolManager.getProjectOwnedAgentPools(projectId))
        modelAndView.model.put("agentPools", pools)
        return modelAndView
    }
}