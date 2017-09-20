package jetbrains.buildServer.clouds.ecs

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 20.09.17.
 */
interface EcsCloudClientParameters {
    val imagesData: List<EcsCloudImageData>
    val instanceLimit: Int
}

