resource "aws_ecs_cluster" "default" {
  name = "${var.project_name}-${var.stack_name}"
}

resource "aws_ecs_task_definition" "default" {
  family = "${var.project_name}-agent-${var.stack_name}"

  container_definitions = <<DEFINITION
[
    {
      "volumesFrom": [],
      "cpu": ${var.ecs_task_cpu},
      "memory": ${var.ecs_task_memory},
      "extraHosts": null,
      "dnsServers": null,
      "disableNetworking": null,
      "dnsSearchDomains": null,
      "portMappings": [],
      "hostname": null,
      "essential": true,
      "entryPoint": null,
      "mountPoints": [],
      "name": "${var.project_name}-${var.stack_name}-${replace(var.app_version,".","-")}",
      "ulimits": null,
      "dockerSecurityOptions": null,
      "environment": [],
      "links": null,
      "workingDirectory": null,
      "readonlyRootFilesystem": null,
      "image": "${var.app_image}:${var.app_version}",
      "command": null,
      "user": null,
      "dockerLabels": null,
      "logConfiguration": {
          "logDriver": "awslogs",
          "options": {
              "awslogs-group": "/aws/ecs/${var.project_name}-agent-${var.stack_name}",
              "awslogs-region": "${var.aws_region}",
              "awslogs-stream-prefix": "${var.project_name}"
          }
      },
      "privileged": null,
      "memoryReservation": null
    }
  ]
DEFINITION
}

resource "aws_cloudwatch_log_group" "default" {
  name              = "/aws/ecs/${var.project_name}-agent-${var.stack_name}"
  retention_in_days = "${var.log_retention}"

  tags {
    Terraform   = "true"
    Environment = "${var.stack_name}"
    Application = "${var.project_name}"
  }
}
