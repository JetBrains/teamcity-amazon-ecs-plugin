

package jetbrains.buildServer.clouds.ecs.apiConnector

import com.amazonaws.services.ecs.model.LaunchType

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 19.09.17.
 */
interface EcsApiConnector {
    fun listTaskDefinitions(): List<String> //list of task definition arns
    fun describeTaskDefinition(taskDefinitionArn: String): EcsTaskDefinition?

    fun runTask(launchType: LaunchType?,
                taskDefinition: EcsTaskDefinition,
                cluster: String?,
                taskGroup: String?,
                subnets: Collection<String>,
                securityGroups: Collection<String>,
                assignPublicIp: Boolean,
                additionalEnvironment: Map<String, String>,
                startedBy: String?,
                fargatePlatformVersion: String?): List<EcsTask>

    fun stopTask(task: String, cluster: String?, reason: String?)

    fun listRunningTasks(cluster: String?, startedBy: String?): List<String> //list of task arns
    fun listStoppedTasks(cluster: String?, startedBy: String?): List<String> //list of task arns
    fun describeTask(taskArn:String, cluster: String?): EcsTask?

    fun listClusters(): List<String> //list of cluster arns
    fun describeCluster(clusterArn:String): EcsCluster?
    fun testConnection(): TestConnectionResult

    fun getMaxCPUReservation(cluster: String?, period: Int): Int
}

class TestConnectionResult(val message: String?, val success: Boolean) {
}