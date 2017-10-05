package jetbrains.buildServer.clouds.ecs.web

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.BuildProject
import jetbrains.buildServer.controllers.BaseFormXmlController
import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.internal.PluginPropertiesUtil
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
                               web: WebControllerManager,
                               private val taskDefsController: EcsTaskDefinitionChooserController,
                               private val clustersController: EcsClusterChooserController) : BaseFormXmlController() {
    private val LOG = Logger.getInstance(EcsProfileEditController::class.java.getName())
    private val url = pluginDescriptor.getPluginResourcesPath(EDIT_ECS_HTML)

    init {
        web.registerController(url, this)
    }

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse, xmlResponse: Element) {
        val propsBean = BasePropertiesBean(null)
        PluginPropertiesUtil.bindPropertiesFromRequest(request, propsBean, true)
        val props = propsBean.properties
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
        modelAndView.model.put("taskDefChooserUrl", taskDefsController.url)
        modelAndView.model.put("clusterChooserUrl", clustersController.url)
        return modelAndView
    }
}