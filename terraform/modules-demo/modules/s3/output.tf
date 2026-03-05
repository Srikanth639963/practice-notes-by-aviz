output "bucket_arn" {
    description = "My S3 bucket name"
    value = aws_s3_bucket.mybucket.arn
}