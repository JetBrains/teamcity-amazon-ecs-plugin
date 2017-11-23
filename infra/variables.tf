# Project parameters
variable "project_name" {
  default = "teamcity"
}

variable "stack_name" {
  description = "Name of the stack: sandbox/staging/production."
  default     = "example"
}

# EC2 parameters
variable "instance_type" {
  description = "EC2 instance type that will be used for ECS."
  default     = "c3.xlarge"
}

variable "ec2_keypair_name" {
  description = "The key name that should be used for the EC2 instance."
}

variable "vpc_id" {
  description = "The id of the VPC"
}

# Autoscaler parameters
variable "asg_min_size" {
  description = "The minimum size of the auto scale group."
  default     = 1
}

variable "asg_max_size" {
  description = "The maximum size of the auto scale group."
  default     = 3
}

# Agent parameters
variable "app_image" {
  description = "The image used to start a agent."
  default     = "jetbrains/teamcity-agent"
}

variable "app_version" {
  description = "The version of agent image."
  default     = "latest"
}

variable "agent_cpu" {
  description = "The minimum number of CPU units to reserve for the agent."
  default     = 2048
}

variable "agent_mem" {
  description = "The number of MiB of memory to reserve for the agent."
  default     = 3740
}

variable "agent_disk" {
  description = "The size of docker base device, which limits the size of agent."
  default     = "20G"
}
