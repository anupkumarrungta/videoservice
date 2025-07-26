# Test Application Only - Skip CLI Issues
# This script tests the application directly without relying on AWS CLI

Write-Host "=== Application Test Script ===" -ForegroundColor Green
Write-Host "Testing the application directly (bypassing CLI issues)" -ForegroundColor Yellow

# Test 1: Check if application is running
Write-Host "`n1. Checking if application is accessible..." -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/translation/health" -Method GET -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ Application is running and accessible" -ForegroundColor Green
        Write-Host "Response: $($response.Content)" -ForegroundColor Gray
    } else {
        Write-Host "✗ Application returned status: $($response.StatusCode)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ Application is not running or not accessible" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Gray
    Write-Host "`nTo start the application:" -ForegroundColor Yellow
    Write-Host "1. Run: mvn spring-boot:run" -ForegroundColor White
    Write-Host "2. Or use your IDE to run VideoTranslationApplication" -ForegroundColor White
}

# Test 2: Check AWS credentials in environment
Write-Host "`n2. Checking AWS environment variables..." -ForegroundColor Cyan
$envVars = @("AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY", "AWS_REGION")
foreach ($var in $envVars) {
    $value = [Environment]::GetEnvironmentVariable($var)
    if ($value) {
        Write-Host "✓ $var is set" -ForegroundColor Green
    } else {
        Write-Host "✗ $var is not set" -ForegroundColor Red
    }
}

# Test 3: Test network connectivity to AWS services
Write-Host "`n3. Testing network connectivity to AWS services..." -ForegroundColor Cyan
$awsServices = @(
    "transcribe.us-east-1.amazonaws.com",
    "translate.us-east-1.amazonaws.com", 
    "polly.us-east-1.amazonaws.com",
    "s3.us-east-1.amazonaws.com"
)

foreach ($service in $awsServices) {
    try {
        $pingTest = Test-NetConnection -ComputerName $service -Port 443 -InformationLevel Quiet
        if ($pingTest) {
            Write-Host "✓ $service is accessible" -ForegroundColor Green
        } else {
            Write-Host "✗ $service is not accessible" -ForegroundColor Red
        }
    } catch {
        Write-Host "✗ Failed to test $service" -ForegroundColor Red
    }
}

# Test 4: Check S3 bucket access (using PowerShell)
Write-Host "`n4. Testing S3 bucket access..." -ForegroundColor Cyan
try {
    # This will test if the bucket is accessible
    $bucketTest = aws s3 ls s3://video-translation-bucket2 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ S3 bucket is accessible" -ForegroundColor Green
        Write-Host "Bucket contents:" -ForegroundColor Gray
        Write-Host $bucketTest -ForegroundColor Gray
    } else {
        Write-Host "✗ S3 bucket access failed" -ForegroundColor Red
        Write-Host $bucketTest -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ Failed to test S3 bucket" -ForegroundColor Red
}

Write-Host "`n=== Application Test Summary ===" -ForegroundColor Green
Write-Host "If the application is running and network connectivity is good," -ForegroundColor Yellow
Write-Host "the AWS SDK should work even if CLI has issues." -ForegroundColor Yellow

Write-Host "`nNext Steps:" -ForegroundColor Cyan
Write-Host "1. Start your application if not running" -ForegroundColor White
Write-Host "2. Upload a short test video (30 seconds) with English audio" -ForegroundColor White
Write-Host "3. Monitor application logs for AWS SDK messages" -ForegroundColor White
Write-Host "4. Look for real transcription results (not mock data)" -ForegroundColor White

Write-Host "`nExpected log messages when working:" -ForegroundColor Cyan
Write-Host "- [Transcription] Starting AWS Transcribe for file:" -ForegroundColor Gray
Write-Host "- [Transcription] Transcription job started successfully:" -ForegroundColor Gray
Write-Host "- [Transcription] SUCCESS: Transcription completed!" -ForegroundColor Gray
Write-Host "- [Translation Pipeline] Transcript for chunk X: [real text]" -ForegroundColor Gray 