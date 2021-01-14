/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.clouds.ecs.web

import jetbrains.buildServer.clouds.server.CloudManagerBase
import jetbrains.buildServer.controllers.BaseController
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 12.10.17.
 */
class EcsDeleteImageDialogController(web: WebControllerManager,
                                     private val pluginDescriptor: PluginDescriptor,
                                     private val cloudManager: CloudManagerBase) : BaseController() {
    val url: String
        get() = pluginDescriptor.getPluginResourcesPath(URL)

    init {
        web.registerController(url, this)
    }

    @Throws(Exception::class)
    override fun doHandle(httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse): ModelAndView? {
        val projectId = httpServletRequest.getParameter("projectId")
        val profileId = httpServletRequest.getParameter("profileId")
        val imageId = httpServletRequest.getParameter("imageId")
        if (StringUtil.isEmpty(imageId)) return null

        val client = cloudManager.getClientIfExistsByProjectExtId(projectId, profileId)
        val image = client.findImageById(imageId)

        if (BaseController.isGet(httpServletRequest)) {
            val modelAndView = ModelAndView(pluginDescriptor.getPluginResourcesPath("deleteImageDialog.jsp"))
            modelAndView.modelMap.put("instances", if (image == null) emptyList<Any>() else image.instances)
            return modelAndView
        } else if (isPost(httpServletRequest) && image != null) {
            for (instance in image.instances) {
                client.terminateInstance(instance)
            }
        }
        return null
    }

    companion object {
        val URL = "deleteKubeImage.html"
    }
}
