package jetbrains.buildServer.clouds.ecs.web

import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnectorImpl
import jetbrains.buildServer.clouds.ecs.toAwsCredentials
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.internal.PluginPropertiesUtil
import jetbrains.buildServer.util.amazon.AWSCommonParams
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private const val ECS_TASK_DEFS_HTML = "ecsTaskDefs.html"

class EcsTaskDefinitionChooserController(private val pluginDescriptor: PluginDescriptor,
                                  web: WebControllerManager) : BaseController() {
    val url = pluginDescriptor.getPluginResourcesPath(ECS_TASK_DEFS_HTML)

    init {
        web.registerController(url, this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val propsBean = BasePropertiesBean(null)
        PluginPropertiesUtil.bindPropertiesFromRequest(request, propsBean, true)
        val props = propsBean.properties

        val api = EcsApiConnectorImpl(props.toAwsCredentials(), AWSCommonParams.getRegionName(props))

        val modelAndView = ModelAndView(pluginDescriptor.getPluginResourcesPath("taskDefs.jsp"))
        modelAndView.model["taskDefs"] = api.listTaskDefinitions()
                .mapNotNull { taskDefArn -> api.describeTaskDefinition(taskDefArn) }
                .sortedBy { taskDef -> taskDef.displayName }
        return modelAndView
    }
}