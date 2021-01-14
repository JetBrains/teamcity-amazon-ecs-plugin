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

import com.amazonaws.services.ecs.model.LaunchType
import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.BuildProject
import jetbrains.buildServer.clouds.ecs.EcsParameterConstants
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
        val mv = ModelAndView(pluginDescriptor.getPluginResourcesPath("editProfile.jsp"))
        val projectId = request.getParameter("projectId")
        val pools = ArrayList<AgentPool>()
        if (BuildProject.ROOT_PROJECT_ID != projectId) {
            pools.add(AgentPoolUtil.DUMMY_PROJECT_POOL)
        }
        pools.addAll(agentPoolManager.getProjectOwnedAgentPools(projectId))
        mv.model.put("launchTypes", LaunchType.values().toMutableList())
        mv.model["fargateVersions"] = EcsParameterConstants.FARGATE_VERSIONS.toMutableList()
        mv.model.put("agentPools", pools)
        mv.model.put("taskDefChooserUrl", taskDefsController.url)
        mv.model.put("clusterChooserUrl", clustersController.url)
        mv.model.put("deleteImageUrl", deleteImageDialogController.url)
        mv.model.put("testConnectionUrl", url + "?testConnection=true")
        return mv
    }
}