# AWS ECS Terraform example

This is a Terraform example which creates an [ECS cluster](modules/ecs) over the [EC2 AutoScaling group](modules/ec2) for TeamCity agents.

## Description

### Capacity planning

By default, the autoscaler sets up [c3.xlarge](variables.tf#L14) instances containing 4096 CPU units and [50GB disk](variables.tf#L23).
The Agent container reserves [2048 CPU units](variables.tf#L54) and [20GB disk](variables.tf#L64).
So an instance may contain up to two agents.

Allocating disk space is important. The Docker parameter (`--storage-opt dm.basesize=20`) is used for this limitation. If you don't use the "Amazon ECS-Optimized Amazon Linux AMI", you need to rewrite the user data of the [Launch Configuration](modules/ec2/main.tf#L21-L39).

Before customizing these parameters, the following requirements must be met:
* The instance CPU must be utilized by agents 100%.
* The instance disk space must be sufficient for agent containers and Docker images.

### Autoscale

CloudWatch observes ECS metrics. If the CPU Reservation metric equals 100%, AutoScaler will scale-out.
If it is less than 100%, the AutoScaler will scale-in.

Scaling-out is much simpler than scaling-in. 

All instances have Scale-In protection, and AutoScaler always tries to make a Scale-In.
[CloudWatch](modules/lambda/main.tf#L64-L82) monitors ECS events and runs the [Lambda unprotect function](modules/lambda/ecs-unprotect-lambda/index.py).
This function removes the Scale-In protection from instances without ECS tasks, but keeps the number of instances equal to the minimum number of instances in the AutoScaling group. Thus, we remove unused instances and keep some instances for future.

You can customize the retain number in [Lambda module](modules/lambda/main.tf#L59).


### Security

This example contains IAM policies for Lambda, ec2 instances. 
We also create the server account to run tasks on the ECS cluster.

### Build Agent logs

ECS forward agent logs to the CloudWatch Log group `/aws/ecs/${var.project_name}-agent-${var.stack_name}`
You can configure logdriver in the [ECS module](modules/ecs/main.tf#L34-L41)

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

Apply the Terraform infrastructure and you get the ECS plug-in settings in outputs:
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

Paste these outputs into the ECS Cloud profile and run ECS Cloud agents.

Happy building with TeamCity!

## License

Apache 2. See [LICENSE](LICENSE) for full details.
