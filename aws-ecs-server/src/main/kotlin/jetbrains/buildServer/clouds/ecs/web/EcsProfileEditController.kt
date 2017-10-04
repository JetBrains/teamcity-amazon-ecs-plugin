package jetbrains.buildServer.clouds.ecs.web

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
                               web: WebControllerManager) : BaseFormXmlController() {
    private val LOG = Logger.getInstance(EcsProfileEditController::class.java.getName())
    private val url = pluginDescriptor.getPluginResourcesPath(EDIT_ECS_HTML)

    init {
        web.registerController(url, this)
    }

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse, xmlResponse: Element) {
        val propsBean = BasePropertiesBean(null)
        PluginPropertiesUtil.bindPropertiesFromRequest(request, propsBean, true)
        val props = propsBean.properties

        if (request.getParameter("loadData").toBoolean()){
            val api = EcsApiConnectorImpl(props.toAwsCredentials(), AWSCommonParams.getRegionName(props))
            try {
                val taskDefsElement = Element("taskDefs")
                for(taskDef in api.listTaskDefinitions().mapNotNull { taskDefArn -> api.describeTaskDefinition(taskDefArn) }){
                    val element = Element("taskDef")
                    element.setAttribute("id", taskDef.arn)
                    element.setAttribute("text", taskDef.family)
                    taskDefsElement.addContent(element)
                }
                val clustersElement = Element("clusters")
                for(cluster in api.listClusters().mapNotNull { clusterArn -> api.describeCluster(clusterArn) }){
                    val element = Element("cluster")
                    element.setAttribute("id", cluster.arn)
                    element.setAttribute("text", cluster.name)
                    clustersElement.addContent(element)
                }
                xmlResponse.addContent(taskDefsElement)
                xmlResponse.addContent(clustersElement)
            } catch (ex: Exception){
                LOG.debug(ex)
                val errors = ActionErrors()
                errors.addError("loadData", ex.message)
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
        modelAndView.model.put("agentPools", pools)
        modelAndView.model.put("imageDataLoadUrl", url + "?loadData=true")
        return modelAndView
    }
}