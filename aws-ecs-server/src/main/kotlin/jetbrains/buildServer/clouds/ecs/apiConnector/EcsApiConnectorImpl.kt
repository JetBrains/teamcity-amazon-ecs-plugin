/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.clouds.ecs.apiConnector

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder
import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest
import com.amazonaws.services.cloudwatch.model.Statistic
import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.AmazonECSClientBuilder
import com.amazonaws.services.ecs.model.*
import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.serverSide.TeamCityProperties
import jetbrains.buildServer.version.ServerVersionHolder
import java.util.*
import java.util.concurrent.TimeUnit


class EcsApiConnectorImpl(awsCredentials: AWSCredentials?, awsRegion: String?) : EcsApiConnector {
    private val LOG = Logger.getInstance(EcsApiConnectorImpl::class.java.getName())
    private val ecs: AmazonECS
    private val cloudWatch: AmazonCloudWatch

    init {
        val clientConfig = ClientConfiguration().withUserAgentPrefix("JetBrains TeamCity " + ServerVersionHolder.getVersion().displayVersion)

        val httpProxy = TeamCityProperties.getProperty("teamcity.ecs.https.proxyHost", TeamCityProperties.getProperty("teamcity.https.proxyHost"))
        val httpProxyPort = TeamCityProperties.getInteger("teamcity.ecs.https.proxyPort", TeamCityProperties.getInteger("teamcity.https.proxyPort", -1))
        val httpProxyUser = TeamCityProperties.getProperty("teamcity.ecs.https.proxyLogin", TeamCityProperties.getProperty("teamcity.https.proxyLogin"))
        val httpProxyPassword = TeamCityProperties.getProperty("teamcity.ecs.https.proxyPassword", TeamCityProperties.getProperty("teamcity.https.proxyPassword"))

        LOG.debug(String.format("ECS client proxy settings: proxy - %s, port - %d, user - %s", httpProxy, httpProxyPort, httpProxyUser, httpProxyPassword))

        if (!httpProxy.isEmpty()){
           clientConfig.setProxyHost(httpProxy)
        }

        if (httpProxyPort >= 0){
            clientConfig.setProxyPort(httpProxyPort)
        }

        if (!httpProxyUser.isEmpty()){
            clientConfig.setProxyUsername(httpProxyUser)
        }

        if (!httpProxyPassword.isEmpty()){
            clientConfig.setProxyPassword(httpProxyPassword)
        }

        val ecsBuilder = AmazonECSClientBuilder
                .standard()
                .withClientConfiguration(clientConfig)
                .withRegion(awsRegion)
        if(awsCredentials != null){
            ecsBuilder.withCredentials(object: AWSCredentialsProvider{
                override fun getCredentials(): AWSCredentials {
                    return awsCredentials
                }

                override fun refresh() {
                    //no-op
                }
            })
        }
        ecs = ecsBuilder.build()

        var cloudWatchBuilder = AmazonCloudWatchClientBuilder.standard()
                .withClientConfiguration(clientConfig)
                .withRegion(awsRegion)
        if(awsCredentials != null){
            cloudWatchBuilder.withCredentials(object: AWSCredentialsProvider{
                override fun getCredentials(): AWSCredentials {
                    return awsCredentials
                }

                override fun refresh() {
                    //no-op
                }
            })
        }
        cloudWatch = cloudWatchBuilder.build()
    }

    override fun runTask(
            launchType: LaunchType?,
            taskDefinition: EcsTaskDefinition,
            cluster: String?,
            taskGroup: String?,
            subnets: Collection<String>,
            securityGroups: Collection<String>,
            assignPublicIp: Boolean,
            additionalEnvironment: Map<String, String>,
            startedBy: String?,
            fargatePlatformVersion: String?
    ): List<EcsTask> {
        val containerOverrides = taskDefinition.containers.map {
            containerName -> ContainerOverride()
                                .withName(containerName)
                                .withEnvironment(additionalEnvironment.entries.map { entry -> KeyValuePair().withName(entry.key).withValue(entry.value) })
        }

        var request = RunTaskRequest()
                .withTaskDefinition(taskDefinition.arn)
                .withOverrides(TaskOverride().withContainerOverrides(containerOverrides))
                .withStartedBy(startedBy)
        if (launchType != null){
            request.withLaunchType(launchType)
        }
        if (launchType != LaunchType.EC2 && fargatePlatformVersion != null){
            request.withPlatformVersion(fargatePlatformVersion)
        }
        if(cluster != null && !cluster.isEmpty()) request = request.withCluster(cluster)
        if(taskGroup != null && !taskGroup.isEmpty()) request = request.withGroup(taskGroup)
        if(!subnets.isEmpty() || !securityGroups.isEmpty()) request = request.withNetworkConfiguration(
                NetworkConfiguration().withAwsvpcConfiguration(
                        AwsVpcConfiguration()
                                .let { if (subnets.isNotEmpty()) it.withSubnets(subnets).withAssignPublicIp(if(assignPublicIp) AssignPublicIp.ENABLED else AssignPublicIp.DISABLED) else it }
                                .let { if (securityGroups.isNotEmpty()) it.withSecurityGroups(securityGroups) else it }))

        val runTaskResult = ecs.runTask(request)
        if (!runTaskResult.failures.isEmpty())
            throw EcsApiCallFailureException(runTaskResult.failures)

        return runTaskResult.tasks.map { it.wrap() }
    }

    override fun listTaskDefinitions(): List<String> {
        var taskDefArns:List<String> = ArrayList()
        var nextToken: String? = null;
        do{
            var request = ListTaskDefinitionsRequest()
            if(nextToken != null) request = request.withNextToken(nextToken)
            val taskDefsResult = ecs.listTaskDefinitions(request)
            taskDefArns = taskDefArns.plus(taskDefsResult.taskDefinitionArns)
            nextToken = taskDefsResult.nextToken
        }
        while(nextToken != null)
        return taskDefArns
    }

    override fun describeTaskDefinition(taskDefinitionArn: String): EcsTaskDefinition? {
        return ecs.describeTaskDefinition(DescribeTaskDefinitionRequest().withTaskDefinition(taskDefinitionArn)).taskDefinition.wrap()
    }

    override fun stopTask(task: String, cluster: String?, reason: String?) {
        ecs.stopTask(StopTaskRequest().withTask(task).withCluster(cluster))
    }

    override fun listRunningTasks(cluster: String?, startedBy: String?): List<String> {
        return listTasks(cluster, startedBy, DesiredStatus.RUNNING)
    }

    override fun listStoppedTasks(cluster: String?, startedBy: String?): List<String> {
        return listTasks(cluster, startedBy, DesiredStatus.STOPPED)
    }

    private fun listTasks(cluster: String?, startedBy: String?, desiredStatus:DesiredStatus): List<String> {
        var taskArns:List<String> = ArrayList()
        var nextToken: String? = null;
        do{
            var listTasksRequest = ListTasksRequest()
                    .withCluster(cluster)
                    .withStartedBy(startedBy)
                    .withDesiredStatus(desiredStatus)

            if(nextToken != null) listTasksRequest = listTasksRequest.withNextToken(nextToken)

            val tasksResult = ecs.listTasks(listTasksRequest)
            taskArns = taskArns.plus(tasksResult.taskArns)
            nextToken = tasksResult.nextToken
        }
        while(nextToken != null)
        return taskArns
    }

    override fun describeTask(taskArn: String, cluster: String?): EcsTask? {
        try {
            val tasksResult = ecs.describeTasks(DescribeTasksRequest().withTasks(taskArn).withCluster(cluster))
            if (!tasksResult.failures.isEmpty())
                throw EcsApiCallFailureException(tasksResult.failures)

            return tasksResult.tasks[0]?.wrap()
        } catch (ex:Throwable){
            LOG.warnAndDebugDetails("Unable find task $taskArn in cluster $cluster", ex)
            return null
        }
    }

    override fun listClusters(): List<String> {
        var clusterArns:List<String> = ArrayList()
        var nextToken: String? = null
        do{
            var request = ListClustersRequest()
            if(nextToken != null) request = request.withNextToken(nextToken)
            val tasksResult = ecs.listClusters(request)
            clusterArns = clusterArns.plus(tasksResult.clusterArns)
            nextToken = tasksResult.nextToken
        }
        while(nextToken != null)
        return clusterArns
    }

    override fun describeCluster(clusterArn: String): EcsCluster? {
        val describeClustersResult = ecs.describeClusters(DescribeClustersRequest().withClusters(clusterArn))
        if (!describeClustersResult.failures.isEmpty())
            throw EcsApiCallFailureException(describeClustersResult.failures)

        return describeClustersResult.clusters[0]?.wrap()
    }

    override fun testConnection(): TestConnectionResult {
        try {
            ecs.listClusters()
            return TestConnectionResult("Connection successful", true)
        } catch (ex: Exception){
            return TestConnectionResult(ex.localizedMessage, false)
        }
    }

    override fun getMaxCPUReservation(cluster: String?, period:Int): Int {
        val currentTimeMillis = System.currentTimeMillis()
        val request = GetMetricStatisticsRequest()
                .withMetricName("CPUReservation")
                .withNamespace("AWS/ECS")
                .withDimensions(Dimension().withName("ClusterName").withValue(cluster))
                .withStatistics(Statistic.Maximum)
                .withStartTime(Date(currentTimeMillis - TimeUnit.MINUTES.toMillis(period.toLong() * 2)))
                .withEndTime(Date(currentTimeMillis))
                .withPeriod(period * 60)
        val datapoints = cloudWatch.getMetricStatistics(request).datapoints
        if(datapoints.isEmpty()) return -1
        return datapoints[0].maximum.toInt()
    }
}