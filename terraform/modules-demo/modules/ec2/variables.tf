variable "ami" {
  description = "AMI id for ec2 instance"
  type = string
}

variable "instance_type" {
  description = "Choose your instance capacity"
  type = string
  default = "t3.small"
}

variable "name" {
    description = "name Tag for Instance"
}