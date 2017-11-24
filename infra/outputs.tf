output "aws_access_key_id" {
  value = "${module.iam.aws_access_key_id}"
}

output "aws_secret_access_key" {
  value = "${module.iam.aws_secret_access_key}"
}

output "ecs_cluster_name" {
  value = "${module.ecs.ecs_cluster_name}"
}

output "ecs_taskdefinition_name" {
  value = "${module.ecs.ecs_taskdefinition_name}"
}
