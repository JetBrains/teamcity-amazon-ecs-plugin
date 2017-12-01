#########
# Lambda
#########

data "archive_file" "ecs-scaledown-file" {
  source_file = "${path.module}/ecs-scaledown-lambda/index.py"
  output_path = "ecs-scaledown-lambda.zip"
  type        = "zip"
}

resource "aws_lambda_function" "ecs-asg" {
  function_name    = "${var.project_name}-${var.stack_name}-ecs-asg"
  role             = "${var.iam_role_lambda_ecs_asg_arn}"
  handler          = "index.lambda_handler"
  runtime          = "python2.7"
  timeout          = 300
  filename         = "${data.archive_file.ecs-scaledown-file.output_path}"
  source_code_hash = "${data.archive_file.ecs-scaledown-file.output_base64sha256}"
}

resource "aws_lambda_permission" "allow_sns" {
  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.ecs-asg.function_name}"
  principal     = "sns.amazonaws.com"
  source_arn    = "${var.sns_topic_asg_arn}"
}

resource "aws_sns_topic_subscription" "lambda-sns" {
  topic_arn = "${var.sns_topic_asg_arn}"
  protocol  = "lambda"
  endpoint  = "${aws_lambda_function.ecs-asg.arn}"
}

resource "aws_cloudwatch_log_group" "ecs-asg" {
  name              = "/aws/lambda/${var.project_name}-${var.stack_name}-ecs-asg"
  retention_in_days = "${var.log_retention}"
}

data "archive_file" "ecs-unprotect-file" {
  source_file = "${path.module}/ecs-unprotect-lambda/index.py"
  output_path = "ecs-unprotect-lambda.zip"
  type        = "zip"
}

resource "aws_lambda_function" "ecs-asg-unprotect" {
  function_name    = "${var.project_name}-${var.stack_name}-ecs-unprotect-asg"
  role             = "${var.iam_role_lambda_ecs_unprotect_asg_arn}"
  handler          = "index.lambda_handler"
  runtime          = "python2.7"
  timeout          = 300
  filename         = "${data.archive_file.ecs-unprotect-file.output_path}"
  source_code_hash = "${data.archive_file.ecs-unprotect-file.output_base64sha256}"

  environment {
    variables = {
      ECS_CLUSTER_NAME = "${var.ecs_cluster_name}"
      ASG_GROUP_NAME   = "${var.asg_name}"
      RETAIN_INSTANCES = "${var.asg_min_size}"
    }
  }
}

resource "aws_cloudwatch_event_rule" "unprotect-scheduler" {
  name = "${var.project_name}-${var.stack_name}-unprotect-scheduler"

  event_pattern = <<PATTERN
{
  "source": [
    "aws.ecs"
  ],
  "detail-type": [
    "ECS Task State Change"
  ],
  "detail": {
    "clusterArn": [
      "${var.ecs_cluster_id}"
    ]
  }
}
PATTERN
}

resource "aws_cloudwatch_event_target" "asg-unprotect" {
  rule      = "${aws_cloudwatch_event_rule.unprotect-scheduler.name}"
  target_id = "LambdaEcsAsgUnprotect"
  arn       = "${aws_lambda_function.ecs-asg-unprotect.arn}"
}

resource "aws_lambda_permission" "allow_cloudwatch_to_call_ecs-asg-unprotect" {
  statement_id  = "AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.ecs-asg-unprotect.function_name}"
  principal     = "events.amazonaws.com"
  source_arn    = "${aws_cloudwatch_event_rule.unprotect-scheduler.arn}"
}

resource "aws_cloudwatch_log_group" "ecs-asg-unprotect" {
  name              = "/aws/lambda/${var.project_name}-${var.stack_name}-ecs-unprotect-asg"
  retention_in_days = "${var.log_retention}"
}
