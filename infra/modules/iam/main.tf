resource "aws_iam_role" "ec2_instance_role" {
  assume_role_policy = <<EOT
{
  "Version": "2008-10-17",
  "Statement": [
    {
      "Sid": "",
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOT

  name = "${var.project_name}-${var.stack_name}-ec2InstanceRole"
}

resource "aws_iam_role_policy_attachment" "instance_role" {
  role       = "${aws_iam_role.ec2_instance_role.name}"
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

resource "aws_iam_instance_profile" "instance_profile" {
  name = "${var.project_name}-${var.stack_name}-ec2InstanceProfile"
  role = "${aws_iam_role.ec2_instance_role.name}"
}

# IAM TeamCity ECS User
resource "aws_iam_policy" "server" {
  name        = "${var.project_name}-${var.stack_name}-server"
  path        = "/"
  description = "Policy for ${var.project_name}-${var.stack_name}"

  policy = <<EOT
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": [
                "ecs:RegisterTaskDefinition",
                "ecs:ListClusters",
                "ecs:DescribeContainerInstances",
                "ecs:ListTaskDefinitions",
                "ecs:ListTasks",
                "ecs:DescribeTaskDefinition",
                "cloudwatch:GetMetricStatistics"
            ],
            "Effect": "Allow",
            "Resource": "*"
        },
        {
            "Action": [
                "ecs:DescribeClusters",
                "ecs:StopTask",
                "ecs:ListContainerInstances"
            ],
            "Effect": "Allow",
            "Resource": "arn:aws:ecs:${var.aws_region}:*:cluster/${var.project_name}-${var.stack_name}"
        },
        {
            "Action": [
                "ecs:RunTask"
            ],
            "Effect": "Allow",
            "Resource": [
                "arn:aws:ecs:${var.aws_region}:*:task-definition/${var.project_name}-agent-${var.stack_name}:*"
            ]
        },
        {
            "Action": [
                "ecs:StopTask",
                "ecs:DescribeTasks"
            ],
            "Effect": "Allow",
            "Resource": "arn:aws:ecs:${var.aws_region}:*:task/*"
        }
    ]
}
EOT
}

resource "aws_iam_user" "server" {
  name = "${var.project_name}-${var.stack_name}-server"
}

resource "aws_iam_user_policy_attachment" "server-ecs" {
  policy_arn = "${aws_iam_policy.server.arn}"
  user       = "${aws_iam_user.server.name}"
}

resource "aws_iam_access_key" "server" {
  user = "${aws_iam_user.server.name}"
}

resource "aws_iam_role" "sns-lambda" {
  assume_role_policy = <<EOT
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": "sts:AssumeRole",
            "Principal": {
              "Service": "autoscaling.amazonaws.com"
            },
            "Effect": "Allow",
            "Sid": ""
        }
    ]
}
EOT

  name = "${var.project_name}-${var.stack_name}-sns-lambda"
}

resource "aws_iam_role_policy" "sns-lambda" {
  name = "${var.project_name}-${var.stack_name}-sns-lambda"
  role = "${aws_iam_role.sns-lambda.id}"

  policy = <<EOT
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": [
                "sqs:SendMessage",
                "sqs:GetQueueUrl",
                "sns:Publish"
            ],
            "Effect": "Allow",
            "Resource": "*"
        }
    ]
}
EOT
}

resource "aws_iam_role" "lambda-ecs-asg" {
  name = "${var.project_name}-${var.stack_name}-lambda-ecs-asg"

  assume_role_policy = <<EOT
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": "sts:AssumeRole",
            "Principal": {
              "Service": "lambda.amazonaws.com"
            },
            "Effect": "Allow",
            "Sid": ""
        }
    ]
}
EOT
}

resource "aws_iam_role_policy" "lambda-ecs-asg" {
  name = "${var.project_name}-${var.stack_name}-ecs-asg"
  role = "${aws_iam_role.lambda-ecs-asg.id}"

  policy = <<EOT
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": [
                "autoscaling:CompleteLifecycleAction",
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:PutLogEvents",
                "ec2:DescribeInstances",
                "ec2:DescribeInstanceAttribute",
                "ec2:DescribeInstanceStatus",
                "ec2:DescribeHosts",
                "ecs:ListContainerInstances",
                "ecs:SubmitContainerStateChange",
                "ecs:SubmitTaskStateChange",
                "ecs:DescribeContainerInstances",
                "ecs:UpdateContainerInstancesState",
                "ecs:ListTasks",
                "ecs:DescribeTasks",
                "sns:Publish",
                "sns:ListSubscriptions"
            ],
            "Effect": "Allow",
            "Resource": "*"
        }
    ]
}
EOT
}

resource "aws_iam_role" "lambda-ecs-unprotect-asg" {
  name = "${var.project_name}-${var.stack_name}-lambda-ecs-unprotect-asg"

  assume_role_policy = <<EOT
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": "sts:AssumeRole",
            "Principal": {
              "Service": "lambda.amazonaws.com"
            },
            "Effect": "Allow",
            "Sid": ""
        }
    ]
}
EOT
}

resource "aws_iam_role_policy" "lambda-ecs-unprotect-asg" {
  name = "${var.project_name}-${var.stack_name}-ecs-unprotect-asg"
  role = "${aws_iam_role.lambda-ecs-unprotect-asg.id}"

  policy = <<EOT
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": [
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:PutLogEvents",
                "autoscaling:SetInstanceProtection",
                "autoscaling:DescribeAutoScalingInstances",
                "ecs:ListContainerInstances",
                "ecs:DescribeContainerInstances",
                "ecs:UpdateContainerInstancesState"
            ],
            "Effect": "Allow",
            "Resource": "*"
        }
    ]
}
EOT
}
