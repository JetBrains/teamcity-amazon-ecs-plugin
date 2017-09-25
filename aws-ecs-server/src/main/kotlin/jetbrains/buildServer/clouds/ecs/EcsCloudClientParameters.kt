package jetbrains.buildServer.clouds.ecs

import com.amazonaws.regions.Region

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 20.09.17.
 */
interface EcsCloudClientParameters {
    val region: Region

    val imagesData: List<EcsCloudImageData>
    val instanceLimit: Int
}

