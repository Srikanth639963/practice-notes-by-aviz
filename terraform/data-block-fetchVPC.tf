provider "aws" {
  region = "ap-south-1"
}

# Fetch existing VPC by name tag
data "aws_vpc" "existing" {
  filter {
    name   = "tag:Name"
    values = ["myappnw"]   # ← Name tag of your existing VPC
  }
}

# Fetch existing Subnets inside that VPC by name tag
data "aws_subnet" "web_1a" {
  filter {
    name   = "tag:Name"
    values = ["WEB-CVPC-1A"]
  }
}

data "aws_subnet" "web_1b" {
  filter {
    name   = "tag:Name"
    values = ["WEB-CVPC-1B"]
  }
}

# Launch EC2 inside the fetched VPC and Subnet
resource "aws_instance" "web" {
  ami           = "ami-0d176f79571d18a8f"
  instance_type = "t3.micro"
  subnet_id     = data.aws_subnet.web_1a.id   # ← places EC2 in WEB-CVPC-1A

  tags = {
    Name = "web-in-existing-vpc"
  }
}

output "vpc_id" {
  description = "ID of the existing VPC"
  value       = data.aws_vpc.existing.id
}

output "subnet_1a_id" {
  description = "ID of WEB-CVPC-1A Subnet"
  value       = data.aws_subnet.web_1a.id
}

output "subnet_1b_id" {
  description = "ID of WEB-CVPC-1B Subnet"
  value       = data.aws_subnet.web_1b.id
}

output "instance_id" {
  description = "Launched EC2 Instance ID"
  value       = aws_instance.web.id
}
