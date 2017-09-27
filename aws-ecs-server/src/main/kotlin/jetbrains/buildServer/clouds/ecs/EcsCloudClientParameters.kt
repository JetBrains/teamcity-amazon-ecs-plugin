package jetbrains.buildServer.clouds.ecs

import com.amazonaws.auth.AWSCredentials

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 20.09.17.
 */
interface EcsCloudClientParameters {
    val region: String

    val imagesData: List<EcsCloudImageData>
    val instanceLimit: Int
    val awsCredentials: AWSCredentials?
}

