AWS ECS Terraform example
=========================
Terraform example which creates ECS cluster and autoscalling group for TeamCity agents.

These types of resources are supported:

* [AutoScaling group](https://www.terraform.io/docs/providers/aws/r/autoscaling_group.html)
* [AutoScaling lifecycle hook](https://www.terraform.io/docs/providers/aws/r/autoscaling_lifecycle_hooks.html)
* [AutoScaling notification](https://www.terraform.io/docs/providers/aws/r/autoscaling_notification.html)
* [AutoScaling policy](https://www.terraform.io/docs/providers/aws/r/autoscaling_policy.html)
* [CloudWatch metric alarm](https://www.terraform.io/docs/providers/aws/r/cloudwatch_metric_alarm.html)
* [CloudWatch event rule](https://www.terraform.io/docs/providers/aws/r/cloudwatch_event_rule.html)
* [CloudWatch event target](https://www.terraform.io/docs/providers/aws/r/cloudwatch_event_target.html)
* [CloudWatch log group](https://www.terraform.io/docs/providers/aws/r/cloudwatch_log_group.html)
* [Launch configuration](https://www.terraform.io/docs/providers/aws/r/launch_configuration.html)
* [SNS topic](https://www.terraform.io/docs/providers/aws/r/sns_topic.html)
* [SNS topic subscription](https://www.terraform.io/docs/providers/aws/r/sns_topic_subscription.html)
* [ECS cluster](https://www.terraform.io/docs/providers/aws/r/ecs_cluster.html)
* [ECS task definition](https://www.terraform.io/docs/providers/aws/r/ecs_task_definition.html)
* [IAM access key](https://www.terraform.io/docs/providers/aws/r/iam_access_key.html)
* [IAM instance profile](https://www.terraform.io/docs/providers/aws/r/iam_instance_profile.html)
* [IAM policy](https://www.terraform.io/docs/providers/aws/r/iam_policy.html)
* [IAM role](https://www.terraform.io/docs/providers/aws/r/iam_role.html)
* [IAM role policy](https://www.terraform.io/docs/providers/aws/r/iam_role_policy.html)
* [IAM role policy attachment](https://www.terraform.io/docs/providers/aws/r/iam_role_policy_attachment.html)
* [Lambda function](https://www.terraform.io/docs/providers/aws/r/lambda_function.html)
* [Lambda permission](https://www.terraform.io/docs/providers/aws/r/lambda_permission.html)

Main idea
---------

This example creates ECS cluster over EC2 AutoScalling group. CloudWatch observes ECS metrics.
If CPU Reservation metric equal 100% AutoScaler scale-out. If less then 100% AutoScaler scale-in.

Scale-out is a simple. But Scale-in is not. 

All instances have Scale-In protection. So AutoScaler always try to make a Scale-In.
CloudWatch observes ECS events and runs Lambda unprotect function.
This function removes Scale-In protection from instances without ECS tasks. 
But it keeps the number of instances equal to the minimum number instances in AutoScalling group.
You can customize retain number in [Lambda module](modules/lambda/main.tf#L59).

Thus, we remove unused instances and keeps some instances for future.

Requirements
------------

1) Configured default AWS profile:

    ```bash
    bash-3.2$ cat ~/.aws/credentials
    [default]
    aws_access_key_id = AWSACCESSKEYID
    aws_secret_access_key = AwSsEcReTAcCeSsKeY
    ```
    
2) AWS VPC id: `vpc-123abc45`

3) AWS EC2 KeyPair name: `teamcity-example.pub`

Usage
-----

Apply terraform infrastructure and you get ECS plug-in settings in outputs:
```bash
bash-3.2$ terraform apply -var 'ec2_keypair_name=teamcity-example.pub' -var 'vpc_id=vpc-123abc45'
provider.aws.region
  The region where AWS operations will take place. Examples
  are us-east-1, us-west-2, etc.

  Default: us-east-1
  Enter a value: eu-central-1

data.archive_file.ecs-scaledown-file: Refreshing state...
data.archive_file.ecs-unprotect-file: Refreshing state...
...
...
...
Apply complete! Resources: 34 added, 0 changed, 0 destroyed.

Outputs:

aws_access_key_id = PLUGINACCESSKEYID
aws_secret_access_key = PLUGINSECRETACCESSKEY
ecs_cluster_name = teamcity-example
ecs_taskdefinition_name = teamcity-agent-example

```

Customization
-------------

You can customize agent resources, EC2 instance type and autoscaler property in `variables.tf`


Terraform version
-----------------

Terraform version 0.11.0 or newer is required for this version to work.

License
-------
Apache 2. See LICENSE for full details.
