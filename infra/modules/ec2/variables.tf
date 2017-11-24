variable "project_name" {}
variable "stack_name" {}
variable "iam_role_sns_lambda_arn" {}

variable "instance_type" {
  description = "EC2 instance type that will be used for ECS"
}

variable "ec2_keypair_name" {}

variable "instance_profile_arn" {}

variable "docker_basesize" {}

variable "asg_scaling_adjustment" {
  description = "The number of members by which to scale"
  default     = 1
}

variable "asg_cooldown" {
  description = "ASG cooldown period"
  default     = 120
}

variable "asg_metric_period" {
  description = "ASG up/down metric period"
  default     = 60
}

variable "asg_min_size" {
  description = "Required mix size for ASG"
  default     = 1
}

variable "asg_max_size" {
  description = "Required max size for ASG"
  default     = 3
}

variable "vpc_zone_identifier" {
  type = "list"
}
