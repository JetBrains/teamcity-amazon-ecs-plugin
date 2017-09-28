package jetbrains.buildServer.clouds.ecs.apiConnector

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 19.09.17.
 */
interface EcsApiConnector {
    fun listTaskDefinitions(): List<String> //list of task definition arns
    fun describeTaskDefinition(taskDefinitionArn: String): EcsTaskDefinition?

    fun runTask(taskDefinition: EcsTaskDefinition, cluster: String?, taskGroup: String?, additionalEnvironment: Map<String, String>): List<EcsTask>
    fun stopTask(task: String, cluster: String?, reason: String?)

    fun listTasks(cluster: String?): List<String> //list of task arns
    fun describeTask(taskArn:String, cluster: String?): EcsTask?

    fun listClusters(): List<String> //list of cluster arns
    fun describeCluster(clusterArn:String): EcsCluster?
}

