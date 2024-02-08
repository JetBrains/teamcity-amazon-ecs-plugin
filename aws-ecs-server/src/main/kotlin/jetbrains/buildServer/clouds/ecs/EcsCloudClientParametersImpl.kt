

package jetbrains.buildServer.clouds.ecs

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
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

    override val awsCredentialsProvider: AWSCredentialsProvider
        get() {
            return genericParams.parameters.toAwsCredentialsProvider()
        }
}

fun Map<String, String>.toAwsCredentialsProvider(): AWSCredentialsProvider = getCredentialsProvider(this)