# Debug AWS Issues Script
# This script helps identify why AWS services are failing

Write-Host "=== AWS Issues Debug Script ===" -ForegroundColor Green
Write-Host "This script will help identify why AWS services are failing" -ForegroundColor Yellow

# Test 1: Basic AWS CLI
Write-Host "`n1. Testing AWS CLI installation..." -ForegroundColor Cyan
try {
    $awsVersion = aws --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ AWS CLI is installed: $awsVersion" -ForegroundColor Green
    } else {
        Write-Host "✗ AWS CLI is not working properly" -ForegroundColor Red
        Write-Host "Please install AWS CLI from: https://aws.amazon.com/cli/" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "✗ AWS CLI not found" -ForegroundColor Red
    Write-Host "Please install AWS CLI from: https://aws.amazon.com/cli/" -ForegroundColor Yellow
    exit 1
}

# Test 2: AWS Credentials
Write-Host "`n2. Testing AWS credentials..." -ForegroundColor Cyan
try {
    $callerIdentity = aws sts get-caller-identity 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ AWS credentials are valid" -ForegroundColor Green
        Write-Host $callerIdentity
    } else {
        Write-Host "✗ AWS credentials are invalid or missing" -ForegroundColor Red
        Write-Host $callerIdentity
        Write-Host "`nTo fix this:" -ForegroundColor Yellow
        Write-Host "1. Run: aws configure" -ForegroundColor White
        Write-Host "2. Enter your AWS Access Key ID" -ForegroundColor White
        Write-Host "3. Enter your AWS Secret Access Key" -ForegroundColor White
        Write-Host "4. Enter your default region (e.g., us-east-1)" -ForegroundColor White
        Write-Host "5. Enter your output format (json)" -ForegroundColor White
        exit 1
    }
} catch {
    Write-Host "✗ Failed to test AWS credentials" -ForegroundColor Red
    exit 1
}

# Test 3: S3 Bucket Access
Write-Host "`n3. Testing S3 bucket access..." -ForegroundColor Cyan
try {
    $s3Test = aws s3 ls s3://video-translation-bucket2 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ S3 bucket access successful" -ForegroundColor Green
        Write-Host $s3Test
    } else {
        Write-Host "✗ S3 bucket access failed" -ForegroundColor Red
        Write-Host $s3Test
        Write-Host "`nPossible issues:" -ForegroundColor Yellow
        Write-Host "1. S3 bucket 'video-translation-bucket2' doesn't exist" -ForegroundColor White
        Write-Host "2. Your IAM user doesn't have S3 permissions" -ForegroundColor White
        Write-Host "3. Bucket is in a different region" -ForegroundColor White
        Write-Host "`nTo fix:" -ForegroundColor Yellow
        Write-Host "1. Create the bucket: aws s3 mb s3://video-translation-bucket2" -ForegroundColor White
        Write-Host "2. Or update your IAM permissions to include S3 access" -ForegroundColor White
    }
} catch {
    Write-Host "✗ Failed to test S3 access" -ForegroundColor Red
}

# Test 4: AWS Transcribe Service
Write-Host "`n4. Testing AWS Transcribe service..." -ForegroundColor Cyan
try {
    # Try a simpler command first
    $transcribeTest = aws transcribe help 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ AWS Transcribe CLI is available" -ForegroundColor Green
        
        # Now try the actual service test
        $transcribeListTest = aws transcribe list-transcription-jobs 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ AWS Transcribe service is accessible" -ForegroundColor Green
        } else {
            Write-Host "✗ AWS Transcribe service access failed" -ForegroundColor Red
            Write-Host $transcribeListTest
            Write-Host "`nRequired IAM permissions for Transcribe:" -ForegroundColor Yellow
            Write-Host "- transcribe:StartTranscriptionJob" -ForegroundColor White
            Write-Host "- transcribe:GetTranscriptionJob" -ForegroundColor White
            Write-Host "- transcribe:ListTranscriptionJobs" -ForegroundColor White
            Write-Host "- s3:PutObject (for your bucket)" -ForegroundColor White
            Write-Host "- s3:GetObject (for your bucket)" -ForegroundColor White
        }
    } else {
        Write-Host "✗ AWS Transcribe CLI not available" -ForegroundColor Red
        Write-Host $transcribeTest
    }
} catch {
    Write-Host "✗ Failed to test AWS Transcribe service" -ForegroundColor Red
}

# Test 5: AWS Translate Service
Write-Host "`n5. Testing AWS Translate service..." -ForegroundColor Cyan
try {
    $translateTest = aws translate translate-text --text "Hello" --source-language-code "en" --target-language-code "es" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ AWS Translate service is accessible" -ForegroundColor Green
        Write-Host $translateTest
    } else {
        Write-Host "✗ AWS Translate service access failed" -ForegroundColor Red
        Write-Host $translateTest
        Write-Host "`nRequired IAM permissions for Translate:" -ForegroundColor Yellow
        Write-Host "- translate:TranslateText" -ForegroundColor White
    }
} catch {
    Write-Host "✗ Failed to test AWS Translate service" -ForegroundColor Red
}

# Test 6: AWS Polly Service
Write-Host "`n6. Testing AWS Polly service..." -ForegroundColor Cyan
try {
    $pollyTest = aws polly describe-voices --language-code "en-US" --max-items 1 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ AWS Polly service is accessible" -ForegroundColor Green
    } else {
        Write-Host "✗ AWS Polly service access failed" -ForegroundColor Red
        Write-Host $pollyTest
        Write-Host "`nRequired IAM permissions for Polly:" -ForegroundColor Yellow
        Write-Host "- polly:SynthesizeSpeech" -ForegroundColor White
        Write-Host "- polly:DescribeVoices" -ForegroundColor White
    }
} catch {
    Write-Host "✗ Failed to test AWS Polly service" -ForegroundColor Red
}

# Test 7: Network Connectivity
Write-Host "`n7. Testing network connectivity to AWS..." -ForegroundColor Cyan
try {
    $pingTest = Test-NetConnection -ComputerName "transcribe.us-east-1.amazonaws.com" -Port 443 -InformationLevel Quiet
    if ($pingTest) {
        Write-Host "✓ Network connectivity to AWS Transcribe is working" -ForegroundColor Green
    } else {
        Write-Host "✗ Network connectivity to AWS Transcribe failed" -ForegroundColor Red
        Write-Host "Check your internet connection and firewall settings" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ Failed to test network connectivity" -ForegroundColor Red
}

# Test 8: Environment Variables
Write-Host "`n8. Checking environment variables..." -ForegroundColor Cyan
$envVars = @("AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY", "AWS_REGION", "AWS_DEFAULT_REGION")
foreach ($var in $envVars) {
    $value = [Environment]::GetEnvironmentVariable($var)
    if ($value) {
        Write-Host "✓ $var is set" -ForegroundColor Green
    } else {
        Write-Host "✗ $var is not set" -ForegroundColor Red
    }
}

# Test 9: AWS Config Files
Write-Host "`n9. Checking AWS configuration files..." -ForegroundColor Cyan
$awsConfigPath = "$env:USERPROFILE\.aws\config"
$awsCredentialsPath = "$env:USERPROFILE\.aws\credentials"

if (Test-Path $awsConfigPath) {
    Write-Host "✓ AWS config file exists: $awsConfigPath" -ForegroundColor Green
} else {
    Write-Host "✗ AWS config file not found: $awsConfigPath" -ForegroundColor Red
}

if (Test-Path $awsCredentialsPath) {
    Write-Host "✓ AWS credentials file exists: $awsCredentialsPath" -ForegroundColor Green
} else {
    Write-Host "✗ AWS credentials file not found: $awsCredentialsPath" -ForegroundColor Red
}

Write-Host "`n=== Debug Summary ===" -ForegroundColor Green
Write-Host "If you see any red X marks above, those services need to be fixed." -ForegroundColor Yellow
Write-Host "`nCommon solutions:" -ForegroundColor Cyan
Write-Host "1. Run 'aws configure' to set up credentials" -ForegroundColor White
Write-Host "2. Create the S3 bucket: aws s3 mb s3://video-translation-bucket2" -ForegroundColor White
Write-Host "3. Update IAM permissions for your user/role" -ForegroundColor White
Write-Host "4. Check your AWS region settings" -ForegroundColor White
Write-Host "5. Ensure your AWS account has the required services enabled" -ForegroundColor White 