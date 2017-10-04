package jetbrains.buildServer.clouds.ecs.apiConnector

import com.amazonaws.services.ecs.model.Cluster

interface EcsCluster {
    val arn: String
    val name: String
}

fun Cluster.wrap(): EcsCluster = object : EcsCluster{
    override val arn: String
        get() = this@wrap.clusterArn
    override val name: String
        get() = this@wrap.clusterName
}