variable "project_name" {}
variable "stack_name" {}
variable "iam_role_sns_lambda_arn" {}
variable "iam_role_lambda_ecs_asg_arn" {}
variable "iam_role_lambda_ecs_unprotect_asg_arn" {}
variable "ecs_cluster_name" {}
variable "ecs_cluster_id" {}
variable "sns_topic_asg_arn" {}
variable "asg_name" {}
variable "asg_min_size" {}

variable "log_retention" {
  description = "Specifies the number of days you want to retain log events"
  default     = 1
}
