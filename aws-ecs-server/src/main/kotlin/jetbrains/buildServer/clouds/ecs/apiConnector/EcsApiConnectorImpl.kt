package jetbrains.buildServer.clouds.ecs.apiConnector

import com.amazonaws.services.ecs.AmazonECSClient
import com.amazonaws.services.ecs.model.*

class EcsApiConnectorImpl : EcsApiConnector {
    private val apiClient: AmazonECSClient

    init {
        apiClient = AmazonECSClient()
    }

    override fun runTask(taskDefinition: String, cluster: String?, taskGroup: String?): List<EcsTask> {
        val runTaskResult = apiClient.runTask(RunTaskRequest().withCluster(cluster).withTaskDefinition(taskDefinition))
        if(runTaskResult.failures.isEmpty()) return runTaskResult.tasks
        else throw EcsApiException()
    }

    override fun stopTask(task: String, cluster: String?, reason: String?) {
        apiClient.stopTask(StopTaskRequest().withTask(task).withCluster(cluster))
    }

    override fun listTasks(): List<String> {
        val taskArns:List<String> = ArrayList()
        var nextToken: String? = null;
        do{
            val tasksResult = apiClient.listTasks(ListTasksRequest().withCluster().withNextToken(nextToken))
            taskArns.plus(tasksResult.taskArns)
            nextToken = tasksResult.nextToken
        }
        while(nextToken != null)
        return taskArns
    }

    override fun describeTask(taskArn: String): EcsTask? {
        val tasksResult = apiClient.describeTasks(DescribeTasksRequest().withTasks(taskArn))
        if(tasksResult.failures.isEmpty()) return tasksResult.tasks
        else throw EcsApiException()
    }

    override fun listClusters(): List<String> {
        val clusterArns:List<String> = ArrayList()
        var nextToken: String? = null;
        do{
            val tasksResult = apiClient.listClusters()
            clusterArns.plus(tasksResult.clusterArns)
            nextToken = tasksResult.nextToken
        }
        while(nextToken != null)
        return clusterArns
    }

    override fun describeCluster(clusterArn: String): EcsCluster? {
        val describeClustersResult = apiClient.describeClusters(DescribeClustersRequest().withClusters(clusterArn))
        if(describeClustersResult.failures.isEmpty()) return describeClustersResult.clusters
        else throw EcsApiException()
    }
}

