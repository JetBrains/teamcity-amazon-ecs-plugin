# AWS ECS Terraform example

This is terraform example which creates [ECS cluster](modules/ecs) over [EC2 AutoScalling group](modules/ec2) for TeamCity agents.

## Description

### Capacity planning

By default autoscaler setup [c3.xlarge](variables.tf#L14) instances contains 4096 CPU units and [50GB disk](variables.tf#L23).
Agent container reserves [2048 CPU units](variables.tf#L54) and [20GB disk](variables.tf#L64).
So instance may contains up to two agents.

An important place is the allocation of disk space. We use the docker parameter(`--storage-opt dm.basesize=20`) for this limitation.
Of course, if you don't use "Amazon ECS-Optimized Amazon Linux AMI", you need to rewrite user data of [Launch Configuration](modules/ec2/main.tf#L21-L39).

Before you customize this parameters, you must complete this requirements:
* CPU of instance must be 100% utilized by agents.
* Disk of instance must be enough for agent containers and docker images.

### Autoscale

CloudWatch observes ECS metrics. If CPU Reservation metric equal 100% AutoScaler scale-out.
If less then 100% AutoScaler scale-in.

Scale-out is a simple. But Scale-in is not. 

All instances have Scale-In protection. So AutoScaler always try to make a Scale-In.
[CloudWatch](modules/lambda/main.tf#L64-L82) observes ECS events and runs [Lambda unprotect function](modules/lambda/ecs-unprotect-lambda/index.py).
This function removes Scale-In protection from instances without ECS tasks. 
But it keeps the number of instances equal to the minimum number instances in AutoScalling group.
You can customize retain number in [Lambda module](modules/lambda/main.tf#L59).

Thus, we remove unused instances and keeps some instances for future.

### Security

This example contains IAM policies for Lambda, ec2 instances. 
Also we create server account for run tasks on ECS cluster.

### Build Agent logs

ECS forward agent logs to CloudWatch Log group `/aws/ecs/${var.project_name}-agent-${var.stack_name}`
You can configure logdriver in [ECS module](modules/ecs/main.tf#L34-L41)

## Requirements

* Terraform version 0.11.0 or newer.
* Configured default AWS profile:
    ```bash
    bash-3.2$ cat ~/.aws/credentials
    [default]
    aws_access_key_id = AWSACCESSKEYID
    aws_secret_access_key = AwSsEcReTAcCeSsKeY
    ```
* AWS VPC id: `vpc-123abc45`
* AWS EC2 KeyPair name: `teamcity-example.pub`

## Usage

Apply terraform infrastructure and you get ECS plug-in settings in outputs:
```bash
bash-3.2$ git clone https://github.com/JetBrains/teamcity-amazon-ecs-plugin.git
bash-3.2$ cd teamcity-amazon-ecs-plugin/infra
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

Paste these outputs into ECS Cloud profile and run ECS Cloud agents.

Happy building with TeamCity!

## License

Apache 2. See [LICENSE](LICENSE) for full details.
