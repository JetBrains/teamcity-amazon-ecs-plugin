data "aws_ami" "amazon_ecs_os" {
  most_recent = true

  filter {
    name   = "name"
    values = ["*-amazon-ecs-optimized"]
    values = ["hvm"]
  }

  owners = ["amazon"]
}

resource "aws_launch_configuration" "default" {
  name_prefix   = "${var.project_name}-${var.stack_name}-lc-"
  image_id      = "${data.aws_ami.amazon_ecs_os.id}"
  instance_type = "${var.instance_type}"

  iam_instance_profile = "${var.instance_profile_arn}"
  key_name             = "${var.ec2_keypair_name}"

  user_data = <<USERDATA
Content-Type: multipart/mixed; boundary="==BOUNDARY=="
MIME-Version: 1.0

--==BOUNDARY==
Content-Type: text/cloud-boothook; charset="us-ascii"

# Set Docker daemon options
cloud-init-per once docker_options echo 'OPTIONS="$${OPTIONS} --storage-opt dm.basesize=${var.docker_basesize}"' >> /etc/sysconfig/docker

--==BOUNDARY==
Content-Type: text/x-shellscript; charset="us-ascii"

#!/bin/bash
# Set any ECS agent configuration options
echo ECS_CLUSTER=${var.project_name}-${var.stack_name} >> /etc/ecs/ecs.config

--==BOUNDARY==--
USERDATA

  root_block_device {
    volume_size = "20"
    volume_type = "gp2"
  }

  ebs_block_device {
    device_name = "/dev/xvdcz"
    volume_size = "${var.ec2_volume_size}"
    volume_type = "gp2"
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_autoscaling_group" "agents" {
  name                  = "${var.project_name}-${var.stack_name}-ecs-asg"
  launch_configuration  = "${aws_launch_configuration.default.id}"
  max_size              = "${var.asg_max_size}"
  min_size              = "${var.asg_min_size}"
  protect_from_scale_in = true

  vpc_zone_identifier = ["${var.vpc_zone_identifier}"]

  tag {
    key                 = "Name"
    value               = "${var.project_name}-${var.stack_name}-ecs-node"
    propagate_at_launch = true
  }
}

resource "aws_autoscaling_policy" "agents-scale-up" {
  name                   = "${var.project_name}-${var.stack_name}-agents-scale-up"
  scaling_adjustment     = "${var.asg_scaling_adjustment}"
  adjustment_type        = "ChangeInCapacity"
  cooldown               = "${var.asg_cooldown}"
  autoscaling_group_name = "${aws_autoscaling_group.agents.name}"
}

resource "aws_cloudwatch_metric_alarm" "memory-high" {
  alarm_name          = "${var.project_name}-${var.stack_name}-agents-mem-high"
  period              = "${var.asg_metric_period}"
  evaluation_periods  = "1"
  metric_name         = "CPUReservation"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  threshold           = "100"
  namespace           = "AWS/ECS"
  statistic           = "Average"

  alarm_actions = [
    "${aws_autoscaling_policy.agents-scale-up.arn}",
  ]

  dimensions {
    ClusterName = "${var.project_name}-${var.stack_name}"
  }
}

resource "aws_autoscaling_policy" "agents-scale-down" {
  name                   = "${var.project_name}-${var.stack_name}-agents-scale-down"
  scaling_adjustment     = "-${var.asg_scaling_adjustment}"
  adjustment_type        = "ChangeInCapacity"
  cooldown               = "${var.asg_cooldown}"
  autoscaling_group_name = "${aws_autoscaling_group.agents.name}"
}

resource "aws_cloudwatch_metric_alarm" "memory-low" {
  alarm_name          = "${var.project_name}-${var.stack_name}-agents-mem-low"
  period              = "${var.asg_metric_period}"
  evaluation_periods  = "1"
  metric_name         = "CPUReservation"
  comparison_operator = "LessThanThreshold"
  threshold           = "100"
  namespace           = "AWS/ECS"
  statistic           = "Average"

  alarm_actions = [
    "${aws_autoscaling_policy.agents-scale-down.arn}",
  ]

  dimensions {
    ClusterName = "${var.project_name}-${var.stack_name}"
  }
}

resource "aws_autoscaling_notification" "agents-scale-down" {
  group_names = [
    "${aws_autoscaling_group.agents.name}",
  ]

  notifications = [
    "autoscaling:EC2_INSTANCE_LAUNCH",
    "autoscaling:EC2_INSTANCE_TERMINATE",
    "autoscaling:EC2_INSTANCE_LAUNCH_ERROR",
    "autoscaling:EC2_INSTANCE_TERMINATE_ERROR",
  ]

  topic_arn = "${aws_sns_topic.asg-sns-topic.arn}"
}

resource "aws_sns_topic" "asg-sns-topic" {
  name = "${var.project_name}-${var.stack_name}-ASGSNSTopic"
}

resource "aws_autoscaling_lifecycle_hook" "terminate" {
  name                    = "terminate"
  autoscaling_group_name  = "${aws_autoscaling_group.agents.name}"
  default_result          = "ABANDON"
  heartbeat_timeout       = 5400
  lifecycle_transition    = "autoscaling:EC2_INSTANCE_TERMINATING"
  notification_target_arn = "${aws_sns_topic.asg-sns-topic.arn}"
  role_arn                = "${var.iam_role_sns_lambda_arn}"
}
