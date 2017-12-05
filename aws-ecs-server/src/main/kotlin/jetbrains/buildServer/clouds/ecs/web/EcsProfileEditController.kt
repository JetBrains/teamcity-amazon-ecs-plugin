package jetbrains.buildServer.clouds.ecs.web

import com.amazonaws.services.ecs.model.LaunchType
import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.BuildProject
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnectorImpl
import jetbrains.buildServer.clouds.ecs.toAwsCredentials
import jetbrains.buildServer.controllers.ActionErrors
import jetbrains.buildServer.controllers.BaseFormXmlController
import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.internal.PluginPropertiesUtil
import jetbrains.buildServer.serverSide.agentPools.AgentPool
import jetbrains.buildServer.serverSide.agentPools.AgentPoolManager
import jetbrains.buildServer.serverSide.agentPools.AgentPoolUtil
import jetbrains.buildServer.util.amazon.AWSCommonParams
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
                               private val clustersController: EcsClusterChooserController,
                               private val deleteImageDialogController: EcsDeleteImageDialogController) : BaseFormXmlController() {
    private val LOG = Logger.getInstance(EcsProfileEditController::class.java.getName())
    private val url = pluginDescriptor.getPluginResourcesPath(EDIT_ECS_HTML)

    init {
        web.registerController(url, this)
    }

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse, xmlResponse: Element) {
        if (request.getParameter("testConnection").toBoolean()){
            val propsBean = BasePropertiesBean(null)
            PluginPropertiesUtil.bindPropertiesFromRequest(request, propsBean, true)
            val props = propsBean.properties
            try {
                val api = EcsApiConnectorImpl(props.toAwsCredentials(), AWSCommonParams.getRegionName(props))
                val testConnectionResult = api.testConnection()
                if (!testConnectionResult.success) {
                    val errors = ActionErrors()
                    errors.addError("connection", testConnectionResult.message)
                    writeErrors(xmlResponse, errors)
                }
            } catch (ex: Exception){
                LOG.debug(ex)
                val errors = ActionErrors()
                errors.addError("connection", ex.message)
                writeErrors(xmlResponse, errors)
            }
        }
    }

    override fun doGet(request: HttpServletRequest, response: HttpServletResponse): ModelAndView {
        val modelAndView = ModelAndView(pluginDescriptor.getPluginResourcesPath("editProfile.jsp"))
        val projectId = request.getParameter("projectId")
        val pools = ArrayList<AgentPool>()
        if (BuildProject.ROOT_PROJECT_ID != projectId) {
            pools.add(AgentPoolUtil.DUMMY_PROJECT_POOL)
        }
        pools.addAll(agentPoolManager.getProjectOwnedAgentPools(projectId))
        modelAndView.model.put("launchTypes", LaunchType.values())
        modelAndView.model.put("agentPools", pools)
        modelAndView.model.put("taskDefChooserUrl", taskDefsController.url)
        modelAndView.model.put("clusterChooserUrl", clustersController.url)
        modelAndView.model.put("deleteImageUrl", deleteImageDialogController.url)
        modelAndView.model.put("testConnectionUrl", url + "?testConnection=true")
        return modelAndView
    }
}