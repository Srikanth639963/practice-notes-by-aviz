
resource "aws_instance" "mumbai-server" {
  ami              = var.ami_id
  instance_type    = var.instance_type
  count            = var.instance_count
  disable_api_stop = var.enable_stop_protection
  key_name         = var.key_names
  tags = {
    Name        = var.environment
    Environment = var.environment
  }
}