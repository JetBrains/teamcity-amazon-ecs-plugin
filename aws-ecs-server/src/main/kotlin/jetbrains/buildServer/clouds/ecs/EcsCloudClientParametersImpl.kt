package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.CloudClientParameters

fun CloudClientParameters.toEcsParams() : EcsCloudClientParameters = EcsCloudClientParametersImpl()

class EcsCloudClientParametersImpl : EcsCloudClientParameters {
    override val instanceLimit: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val imagesData: List<EcsCloudImageData>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}