variable "aws_region" {}
variable "project_name" {}
variable "stack_name" {}

variable "app_image" {
  default = "jetbrains/teamcity-agent"
}

variable "app_version" {
  default = "latest"
}

variable "ecs_task_cpu" {
  description = "ECS Task definition cpu allocation"
  default     = 2048
}

variable "ecs_task_memory" {
  description = "ECS Task definition memory allocation"
  default     = 3953
}

variable "log_retention" {
  description = "Specifies the number of days you want to retain log events"
  default     = 1
}