module "iam" {
  source       = "modules/iam"
  aws_region   = "${data.aws_region.current.name}"
  project_name = "${var.project_name}"
  stack_name   = "${var.stack_name}"
}

data "aws_region" "current" {
  current = true
}

data "aws_vpc" "vpc" {
  id = "${var.vpc_id}"
}

data "aws_subnet_ids" "vpc" {
  vpc_id = "${data.aws_vpc.vpc.id}"
}

module "ec2" {
  source                  = "modules/ec2"
  project_name            = "${var.project_name}"
  stack_name              = "${var.stack_name}"
  instance_type           = "${var.instance_type}"
  asg_min_size            = "${var.asg_min_size}"
  asg_max_size            = "${var.asg_max_size}"
  ec2_keypair_name        = "${var.ec2_keypair_name}"
  ec2_volume_size         = "${var.ec2_volume_size}"
  docker_basesize         = "${var.agent_disk}"
  instance_profile_arn    = "${module.iam.instance_profile_arn}"
  iam_role_sns_lambda_arn = "${module.iam.iam_role_sns_lambda_arn}"
  vpc_zone_identifier     = "${data.aws_subnet_ids.vpc.ids}"
}

module "lambda" {
  source                                = "modules/lambda"
  project_name                          = "${var.project_name}"
  stack_name                            = "${var.stack_name}"
  asg_name                              = "${module.ec2.asg_name}"
  asg_min_size                          = "${module.ec2.asg_min_size}"
  ecs_cluster_id                        = "${module.ecs.ecs_cluster_id}"
  ecs_cluster_name                      = "${module.ecs.ecs_cluster_name}"
  sns_topic_asg_arn                     = "${module.ec2.sns_topic_asg_arn}"
  iam_role_sns_lambda_arn               = "${module.iam.iam_role_sns_lambda_arn}"
  iam_role_lambda_ecs_asg_arn           = "${module.iam.iam_role_lambda_ecs_asg_arn}"
  iam_role_lambda_ecs_unprotect_asg_arn = "${module.iam.iam_role_lambda_ecs_unprotect_asg_arn}"
}

module "ecs" {
  source          = "modules/ecs"
  aws_region      = "${data.aws_region.current.name}"
  project_name    = "${var.project_name}"
  stack_name      = "${var.stack_name}"
  ecs_task_cpu    = "${var.agent_cpu}"
  ecs_task_memory = "${var.agent_mem}"
  app_image       = "${var.app_image}"
  app_version     = "${var.app_version}"
}
