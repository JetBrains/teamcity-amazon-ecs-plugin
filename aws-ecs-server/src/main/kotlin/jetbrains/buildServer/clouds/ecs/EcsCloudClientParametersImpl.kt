package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudClientParameters
import jetbrains.buildServer.util.StringUtil

fun CloudClientParameters.toEcsParams() : EcsCloudClientParameters = EcsCloudClientParametersImpl(this)

class EcsCloudClientParametersImpl(private val genericParams: CloudClientParameters) : EcsCloudClientParameters {
    override val instanceLimit: Int
        get() {
            val parameter = genericParams.getParameter(PROFILE_INSTANCE_LIMIT_PARAM)
            return if (StringUtil.isEmpty(parameter)) -1 else Integer.valueOf(parameter)
        }
    override val imagesData: List<EcsCloudImageData>
        get() = genericParams.cloudImages.map { EcsCloudImageData(it) }
}