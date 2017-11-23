output "asg_name" {
  value = "${aws_autoscaling_group.agents.name}"
}

output "asg_min_size" {
  value = "${aws_autoscaling_group.agents.min_size}"
}

output "sns_topic_asg_arn" {
  value = "${aws_sns_topic.asg-sns-topic.arn}"
}
