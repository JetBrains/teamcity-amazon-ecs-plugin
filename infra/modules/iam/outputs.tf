output "instance_profile_arn" {
  value = "${aws_iam_instance_profile.instance_profile.arn}"
}

output "iam_role_lambda_ecs_asg_arn" {
  value = "${aws_iam_role.lambda-ecs-asg.arn}"
}

output "iam_role_lambda_ecs_unprotect_asg_arn" {
  value = "${aws_iam_role.lambda-ecs-unprotect-asg.arn}"
}

output "iam_role_sns_lambda_arn" {
  value = "${aws_iam_role.sns-lambda.arn}"
}

output "aws_access_key_id" {
  value = "${aws_iam_access_key.server.id}"
}

output "aws_secret_access_key" {
  value = "${aws_iam_access_key.server.secret}"
}
