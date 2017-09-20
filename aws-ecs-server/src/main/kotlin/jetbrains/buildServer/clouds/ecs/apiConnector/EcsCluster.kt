package jetbrains.buildServer.clouds.ecs.apiConnector

import com.amazonaws.services.ecs.model.Cluster

interface EcsCluster {

}

fun Cluster.wrap(): EcsCluster = object : EcsCluster{}