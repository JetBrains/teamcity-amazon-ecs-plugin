package jetbrains.buildServer.clouds.ecs.apiConnector

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 19.09.17.
 */
interface EcsApiConnector {
    fun runTask(taskDefinition:String, cluster: String?, taskGroup: String?): List<EcsTask>
    fun stopTask(task: String, cluster: String?, reason: String?)

    fun listTasks(): List<String> //list of task arns
    fun describeTask(taskArn:String): EcsTask?

    fun listClusters(): List<String> //list of cluster arns
    fun describeCluster(clusterArn:String): EcsCluster?
}

