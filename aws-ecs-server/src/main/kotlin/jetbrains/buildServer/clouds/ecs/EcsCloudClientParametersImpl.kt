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

package jetbrains.buildServer.clouds.ecs

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import jetbrains.buildServer.clouds.CloudClientParameters
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.util.amazon.AWSCommonParams
import jetbrains.buildServer.util.amazon.AWSCommonParams.*

fun CloudClientParameters.toEcsParams() : EcsCloudClientParameters = EcsCloudClientParametersImpl(this)

class EcsCloudClientParametersImpl(private val genericParams: CloudClientParameters) : EcsCloudClientParameters {
    override val region: String
        get() = AWSCommonParams.getRegionName(genericParams.parameters)!!

    override val instanceLimit: Int
        get() {
            val parameter = genericParams.getParameter(PROFILE_INSTANCE_LIMIT_PARAM)
            return if (StringUtil.isEmpty(parameter)) -1 else Integer.valueOf(parameter)
        }

    override val imagesData: List<EcsCloudImageData>
        get() = genericParams.cloudImages.map { EcsCloudImageData(it) }

    //NOTE: copy pasted from jetbrains.buildServer.util.amazon.AWSCommonParams

    override val awsCredentials: AWSCredentials?
        get() {
            return genericParams.parameters.toAwsCredentials()
        }
}

fun Map<String, String>.toAwsCredentials(): AWSCredentials? {
    val accessKeyId = getAccessKeyId(this)
    val secretAccessKey = getSecretAccessKey(this)
    val useDefaultCredProvChain = isUseDefaultCredentialProviderChain(this)
    return if(useDefaultCredProvChain)
        null
    else
        BasicAWSCredentials(accessKeyId, secretAccessKey)
}

private fun isUseDefaultCredentialProviderChain(params: Map<String, String>): Boolean {
    return java.lang.Boolean.parseBoolean(params[USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM]) || java.lang.Boolean.parseBoolean(USE_DEFAULT_CREDENTIAL_PROVIDER_CHAIN_PARAM_OLD)
}

private fun getAccessKeyId(params: Map<String, String>): String? {
    return getNewOrOld(params, ACCESS_KEY_ID_PARAM, ACCESS_KEY_ID_PARAM_OLD)
}

private fun getSecretAccessKey(params: Map<String, String>): String? {
    var secretAccessKeyParam = params[SECURE_SECRET_ACCESS_KEY_PARAM]
    if (StringUtil.isNotEmpty(secretAccessKeyParam)) return secretAccessKeyParam

    secretAccessKeyParam = params[SECURE_SECRET_ACCESS_KEY_PARAM_OLD]
    return if (StringUtil.isNotEmpty(secretAccessKeyParam)) secretAccessKeyParam else params[SECRET_ACCESS_KEY_PARAM_OLD]
}

private fun getNewOrOld(params: Map<String, String>, newKey: String, oldKey: String): String? {
    val newVal = params[newKey]
    return if (StringUtil.isNotEmpty(newVal)) newVal else params[oldKey]
}
