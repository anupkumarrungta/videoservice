# Test AWS Credentials and Services
# This script helps verify that AWS credentials are properly configured

Write-Host "Testing AWS Credentials and Services..." -ForegroundColor Green

# Test AWS CLI configuration
Write-Host "`n1. Testing AWS CLI configuration..." -ForegroundColor Yellow
try {
    $awsConfig = aws configure list 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ AWS CLI is configured" -ForegroundColor Green
        Write-Host $awsConfig
    } else {
        Write-Host "✗ AWS CLI configuration failed" -ForegroundColor Red
        Write-Host $awsConfig
    }
} catch {
    Write-Host "✗ AWS CLI not found or not working" -ForegroundColor Red
}

# Test AWS credentials
Write-Host "`n2. Testing AWS credentials..." -ForegroundColor Yellow
try {
    $stsResult = aws sts get-caller-identity 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ AWS credentials are valid" -ForegroundColor Green
        Write-Host $stsResult
    } else {
        Write-Host "✗ AWS credentials are invalid or missing" -ForegroundColor Red
        Write-Host $stsResult
    }
} catch {
    Write-Host "✗ Failed to test AWS credentials" -ForegroundColor Red
}

# Test S3 access
Write-Host "`n3. Testing S3 access..." -ForegroundColor Yellow
try {
    $s3Result = aws s3 ls s3://video-translation-bucket2 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ S3 bucket access successful" -ForegroundColor Green
        Write-Host $s3Result
    } else {
        Write-Host "✗ S3 bucket access failed" -ForegroundColor Red
        Write-Host $s3Result
    }
} catch {
    Write-Host "✗ Failed to test S3 access" -ForegroundColor Red
}

# Test Transcribe service
Write-Host "`n4. Testing AWS Transcribe service..." -ForegroundColor Yellow
try {
    $transcribeResult = aws transcribe list-transcription-jobs --max-items 1 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ AWS Transcribe service is accessible" -ForegroundColor Green
    } else {
        Write-Host "✗ AWS Transcribe service access failed" -ForegroundColor Red
        Write-Host $transcribeResult
    }
} catch {
    Write-Host "✗ Failed to test AWS Transcribe service" -ForegroundColor Red
}

# Test Translate service
Write-Host "`n5. Testing AWS Translate service..." -ForegroundColor Yellow
try {
    $translateResult = aws translate translate-text --text "Hello" --source-language-code "en" --target-language-code "es" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ AWS Translate service is accessible" -ForegroundColor Green
        Write-Host $translateResult
    } else {
        Write-Host "✗ AWS Translate service access failed" -ForegroundColor Red
        Write-Host $translateResult
    }
} catch {
    Write-Host "✗ Failed to test AWS Translate service" -ForegroundColor Red
}

# Test Polly service
Write-Host "`n6. Testing AWS Polly service..." -ForegroundColor Yellow
try {
    $pollyResult = aws polly describe-voices --language-code "en-US" --max-items 1 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ AWS Polly service is accessible" -ForegroundColor Green
    } else {
        Write-Host "✗ AWS Polly service access failed" -ForegroundColor Red
        Write-Host $pollyResult
    }
} catch {
    Write-Host "✗ Failed to test AWS Polly service" -ForegroundColor Red
}

Write-Host "`nAWS Credentials and Services Test Complete!" -ForegroundColor Green
Write-Host "If you see any red X marks above, please check your AWS configuration." -ForegroundColor Yellow 