from __future__ import print_function
import boto3
import logging
import os

logging.basicConfig()
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Establish boto3 session
session = boto3.session.Session()
logger.debug("Session is in region %s ", session.region_name)

ecsClient = session.client(service_name='ecs')
asgClient = session.client('autoscaling')

clusterName = os.getenv('ECS_CLUSTER_NAME', 'teamcity-sandbox')
asgGroupName = os.getenv('ASG_GROUP_NAME', 'teamcity-sandbox-ecs-asg')

def env_to_num(env, default):
    try:
        return int(os.getenv(env, default))
    except ValueError:
        return default
retainInstances = env_to_num('RETAIN_INSTANCES',0)

def lambda_handler(event, context):
    logger.info("Start lambda function")

    # Get list of container instance IDs from the clusterName
    clusterListResp = ecsClient.list_container_instances(cluster=clusterName)

    # Get list of describe container instances from the clusterName
    descrInsts = ecsClient.describe_container_instances(
        cluster=clusterName,
        containerInstances=clusterListResp['containerInstanceArns'],
    )

    # Get ARN list of instances without any tasks
    idleInstances = []
    for containerInstance in descrInsts['containerInstances']:
        runTask = containerInstance['runningTasksCount']
        pendTask = containerInstance['pendingTasksCount']
        ec2InstanceId = containerInstance['ec2InstanceId']
        status = containerInstance['status']
        logger.debug("Instance %s has %s tasks" , ec2InstanceId, runTask + pendTask)
        if status == 'ACTIVE' and runTask + pendTask == 0:
            idleInstances.append(containerInstance)
    logger.info("Cluster %s has %s idle instances", clusterName, len(idleInstances))

    # Save idle instance
    idleInstances = idleInstances[retainInstances:]

    # Delete instance protection for autoscaling group
    for containerInstance in idleInstances:
        logger.info("Unprotect instances %s", containerInstance['ec2InstanceId'])
        # Make API calls to set DRAINING and unset ScaleIn protection
        try:
            response = ecsClient.update_container_instances_state(
                    cluster=clusterName,
                    containerInstances=[containerInstance['containerInstanceArn']],
                    status='DRAINING'
            )
            logger.debug("Response received from update_container_instances_state %s",response)
            response = asgClient.set_instance_protection(
                InstanceIds=[containerInstance['ec2InstanceId']],
                AutoScalingGroupName=asgGroupName,
                ProtectedFromScaleIn=False
            )
            logger.debug("Response received from set_instance_protection %s",response)
        except Exception, e:
            logger.error(str(e))
