variable "instance_type" {
  type        = string
  description = "Enter the EC2 Instance type you want to launch"
}

variable "instance_count" {
  type        = number
  description = "Enter the req instances you want to launch"
}

variable "ami_id" {
  type        = string
  description = "Enter the AMI ID you want to use for launching instances"
  default     = "ami-051a31ab2f4d498f5"
}

variable "enable_stop_protection" {
  type    = bool
}

variable "region_id" {
  type    = string
  default = "ap-south-1"
}


provider "aws" {
  region = var.region_id
}

variable "environment" {
  type = string
}

variable "key_names" {
  type    = string
  default = "jenkins-kp"
}
