package jetbrains.buildServer.clouds.ecs.web

import jetbrains.buildServer.controllers.BaseFormXmlController
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.jdom.Element
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

const val EDIT_ECS_HTML = "editEcs.html"

class EcsProfileEditController(val pluginDescriptor: PluginDescriptor, web: WebControllerManager) : BaseFormXmlController() {
    init {
        web.registerController(pluginDescriptor.getPluginResourcesPath(EDIT_ECS_HTML), this)
    }

    override fun doPost(request: HttpServletRequest, response: HttpServletResponse, xmlResponse: Element) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun doGet(request: HttpServletRequest, response: HttpServletResponse): ModelAndView {
        return ModelAndView(pluginDescriptor.getPluginResourcesPath("editProfile.jsp"))
    }
}