# TeamCity Amazon ECS plugin
[![official JetBrains project](http://jb.gg/badges/official.svg)](https://plugins.jetbrains.com/plugin/10067-amazon-ecs-support) 
[![plugin status]( 
https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:TestDrive_TeamCityAmazonEcsPlugin_Build)/statusIcon.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=TestDrive_TeamCityAmazonEcsPlugin_Build&guest=1)

TeamCity plugin which allows running build agents on an AWS ECS cluster.

## Compatibility

The plugin is compatible with TeamCity 2017.1.x and later.

## Installation

You can [download the plugin](https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:TestDrive_TeamCityAmazonEcsPlugin_Build,tags:release/artifacts/content/aws-ecs.zip) and install it as an [additional TeamCity plugin](https://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins).

## Plugin Configuration

Configure Amazon ECS [Cloud Profile](https://confluence.jetbrains.com/display/TCD10/Agent+Cloud+Profile#AgentCloudProfile-ConfiguringCloudProfile) for your project in the Server Administration UI.

The plugin supports Amazon ECS cluster images to start new tasks with a TeamCity build agent running in one of the containers. The plugin supports the [official TeamCity Build Agent Docker image](https://hub.docker.com/r/jetbrains/teamcity-agent) out of the box. You can use your own image as well.

### Limit Cluster Resources Usage

Specify ECS cloud image advanced setting 'Max cluster CPU reservation' to stop creating new TeamCity cloud instances when cluster is overloaded. This requires additional permission granted to AWS user: cloudwatch:GetMetricStatistics

### Proxy Settings

Use [global server proxy settings](https://confluence.jetbrains.com/pages/viewpage.action?pageId=74845225#HowTo...-ConfigureTeamCitytoUseProxyServerforOutgoingConnections).

Or set plugin specific [internal properties](https://confluence.jetbrains.com/display/TCD10/Configuring+TeamCity+Server+Startup+Properties#ConfiguringTeamCityServerStartupProperties-TeamCityinternalproperties)
 - teamcity.ecs.https.proxyHost
 - teamcity.ecs.https.proxyPort
 - teamcity.ecs.https.proxyLogin
 - teamcity.ecs.https.proxyPassword


## Required IAM Role

Allow following actions to AIM role you use in cloud profile.
- ecs:DescribeClusters
- ecs:DescribeTaskDefinition
- ecs:DescribeTasks
- ecs:ListClusters
- ecs:ListTaskDefinitions
- ecs:ListTasks
- ecs:RunTask
- ecs:StopTask
- (optional) cloudwatch:GetMetricStatistics

## ECS Cluster Setup

Optionaly use [provided Terraform template](infra/README.md) to setup ECS cluster.

## License

Apache 2.0

## Feedback

Please feel free to post feedback in the repository [issues](https://github.com/ekoshkin/teamcity-amazon-ecs-plugin/issues).
