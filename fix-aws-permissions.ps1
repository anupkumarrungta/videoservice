# AWS Permissions Fix Script for Video Translation Service
# This script helps you fix the S3 permissions issue

Write-Host "=== AWS Permissions Fix for Video Translation Service ===" -ForegroundColor Green
Write-Host ""

Write-Host "To fix the S3 permissions issue, follow these steps:" -ForegroundColor Yellow
Write-Host ""

Write-Host "1. Go to AWS IAM Console:" -ForegroundColor Cyan
Write-Host "   https://console.aws.amazon.com/iam/"
Write-Host ""

Write-Host "2. Find your user 'video-translation-service' and click on it" -ForegroundColor Cyan
Write-Host ""

Write-Host "3. Click 'Add permissions' and choose 'Attach policies directly'" -ForegroundColor Cyan
Write-Host ""

Write-Host "4. Create a new policy with the following JSON:" -ForegroundColor Cyan
Write-Host ""

Get-Content "aws-s3-policy.json" | ForEach-Object { Write-Host "   $_" -ForegroundColor White }

Write-Host ""
Write-Host "5. Or use AWS CLI to create and attach the policy:" -ForegroundColor Cyan
Write-Host ""

Write-Host "   # Create the policy" -ForegroundColor Gray
Write-Host "   aws iam create-policy --policy-name VideoTranslationServicePolicy --policy-document file://aws-s3-policy.json" -ForegroundColor Gray
Write-Host ""

Write-Host "   # Attach the policy to your user" -ForegroundColor Gray
Write-Host "   aws iam attach-user-policy --user-name video-translation-service --policy-arn arn:aws:iam::832547844468:policy/VideoTranslationServicePolicy" -ForegroundColor Gray
Write-Host ""

Write-Host "6. Alternative: Use the AWS S3 bucket policy directly:" -ForegroundColor Cyan
Write-Host ""

$bucketPolicy = @"
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowVideoTranslationService",
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::832547844468:user/video-translation-service"
            },
            "Action": [
                "s3:GetObject",
                "s3:PutObject",
                "s3:DeleteObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::video-translation-bucket-anup",
                "arn:aws:s3:::video-translation-bucket-anup/*"
            ]
        }
    ]
}
"@

Write-Host "   # Apply bucket policy" -ForegroundColor Gray
Write-Host "   aws s3api put-bucket-policy --bucket video-translation-bucket-anup --policy '$bucketPolicy'" -ForegroundColor Gray
Write-Host ""

Write-Host "7. After applying permissions, restart your application:" -ForegroundColor Cyan
Write-Host "   ./run-with-env.ps1" -ForegroundColor Gray
Write-Host ""

Write-Host "=== Current Status ===" -ForegroundColor Green
Write-Host "✅ Application is working with local storage fallback" -ForegroundColor Green
Write-Host "❌ S3 uploads are failing due to permissions" -ForegroundColor Red
Write-Host "✅ Translation pipeline is working (mock transcription + real AWS Translate + real AWS Polly)" -ForegroundColor Green
Write-Host ""

Write-Host "The application will continue to work with local storage until S3 permissions are fixed." -ForegroundColor Yellow 