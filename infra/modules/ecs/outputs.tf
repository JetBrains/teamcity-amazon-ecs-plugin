output "ecs_cluster_name" {
  value = "${aws_ecs_cluster.default.name}"
}

output "ecs_cluster_id" {
  value = "${aws_ecs_cluster.default.id}"
}

output "ecs_taskdefinition_name" {
  value = "${aws_ecs_task_definition.default.id}"
}
