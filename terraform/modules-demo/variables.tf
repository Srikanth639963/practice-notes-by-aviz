variable "region" {
  type = string
  default = "ap-south-1"
}

variable "ami" {
  description = "AMI to use with the instance"
  type = string
}

variable "instance_type" {
  description = "Choose the instance type"
  type = string
  default = "t3.micro"
}

variable "name" {
  description = "Enter the instance name"
  type = string
  default = "dev-ec2"
}

variable "bucket_name" {
  description = "Enter the bucket name"
  type = string
}