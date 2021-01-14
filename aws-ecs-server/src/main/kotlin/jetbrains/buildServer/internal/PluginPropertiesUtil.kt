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

package jetbrains.buildServer.internal

import jetbrains.buildServer.controllers.BasePropertiesBean
import jetbrains.buildServer.serverSide.crypt.RSACipher

import javax.servlet.http.HttpServletRequest

//NOTE: copy pasted from PluginPropertiesUtil
object PluginPropertiesUtil {
    private val PROPERTY_PREFIX = "prop:"
    private val ENCRYPTED_PROPERTY_PREFIX = "prop:encrypted:"

    @JvmOverloads
    fun bindPropertiesFromRequest(request: HttpServletRequest, bean: BasePropertiesBean, includeEmptyValues: Boolean = false) {
        bean.clearProperties()

        for (o in request.parameterMap.keys) {
            val paramName = o as String
            if (paramName.startsWith(PROPERTY_PREFIX)) {
                if (paramName.startsWith(ENCRYPTED_PROPERTY_PREFIX)) {
                    setEncryptedProperty(paramName, request, bean, includeEmptyValues)
                } else {
                    setStringProperty(paramName, request, bean, includeEmptyValues)
                }
            }
        }
    }

    private fun setStringProperty(paramName: String, request: HttpServletRequest,
                                  bean: BasePropertiesBean, includeEmptyValues: Boolean) {
        val propName = paramName.substring(PROPERTY_PREFIX.length)
        val propertyValue = request.getParameter(paramName).trim { it <= ' ' }
        if (includeEmptyValues || propertyValue.length > 0) {
            bean.setProperty(propName, toUnixLineFeeds(propertyValue))
        }
    }

    private fun setEncryptedProperty(paramName: String, request: HttpServletRequest,
                                     bean: BasePropertiesBean, includeEmptyValues: Boolean) {
        val propName = paramName.substring(ENCRYPTED_PROPERTY_PREFIX.length)
        val propertyValue = RSACipher.decryptWebRequestData(request.getParameter(paramName))
        if (propertyValue != null && (includeEmptyValues || propertyValue.length > 0)) {
            bean.setProperty(propName, toUnixLineFeeds(propertyValue))
        }
    }

    private fun toUnixLineFeeds(str: String): String {
        return str.replace("\r", "")
    }
}
