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

private const val ECS_CLUSTERS_HTML = "ecsClusters.html"

class EcsClusterChooserController(private val pluginDescriptor: PluginDescriptor,
                                  web: WebControllerManager) : BaseController() {
    val url = pluginDescriptor.getPluginResourcesPath(ECS_CLUSTERS_HTML)

    init {
        web.registerController(url, this)
    }

    override fun doHandle(request: HttpServletRequest, response: HttpServletResponse): ModelAndView? {
        val propsBean = BasePropertiesBean(null)
        PluginPropertiesUtil.bindPropertiesFromRequest(request, propsBean, true)
        val props = propsBean.properties

        val api = EcsApiConnectorImpl(props.toAwsCredentials(), AWSCommonParams.getRegionName(props))

        val modelAndView = ModelAndView(pluginDescriptor.getPluginResourcesPath("clusters.jsp"))
        modelAndView.model["clusters"] = api.listClusters()
                .mapNotNull { clusterArn -> api.describeCluster(clusterArn) }
                .sortedBy { cluster -> cluster.name }
        return modelAndView
    }
}