output "public_ip" {
    description = "Launched instance public ip"
    value = aws_instance.instance.public_ip
}

output "priavte_ip" {
    description = "Launched instance public ip"
    value = aws_instance.instance.private_ip
}