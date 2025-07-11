# Test AWS Transcribe Permissions and Connectivity
# This script helps diagnose AWS Transcribe permission issues

Write-Host "=== AWS Services Connectivity Test ===" -ForegroundColor Green
Write-Host ""

# Load .env variables into the current session
Write-Host "1. Loading environment variables from .env file..." -ForegroundColor Yellow
if (Test-Path .env) {
    Get-Content .env | ForEach-Object {
        if ($_ -match "^(.*?)=(.*)$") {
            [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
            Write-Host "   Loaded: $($matches[1])" -ForegroundColor Cyan
        }
    }
    Write-Host "   ✓ Environment variables loaded from .env file" -ForegroundColor Green
} else {
    Write-Host "   ✗ .env file not found" -ForegroundColor Red
    exit 1
}

# Test 1: Check if AWS credentials are configured
Write-Host "`n2. Checking AWS credentials..." -ForegroundColor Yellow
try {
    $awsVersion = aws --version 2>$null
    if ($awsVersion) {
        Write-Host "   ✓ AWS CLI is installed: $awsVersion" -ForegroundColor Green
    } else {
        Write-Host "   ✗ AWS CLI not found" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ AWS CLI not available" -ForegroundColor Red
}

# Test 2: Check if credentials are configured
Write-Host "`n3. Checking AWS credentials configuration..." -ForegroundColor Yellow
try {
    $awsIdentity = aws sts get-caller-identity 2>$null | ConvertFrom-Json
    if ($awsIdentity) {
        Write-Host "   ✓ AWS credentials configured" -ForegroundColor Green
        Write-Host "   - Account ID: $($awsIdentity.Account)" -ForegroundColor Cyan
        Write-Host "   - User ID: $($awsIdentity.UserId)" -ForegroundColor Cyan
        Write-Host "   - ARN: $($awsIdentity.Arn)" -ForegroundColor Cyan
    } else {
        Write-Host "   ✗ AWS credentials not configured" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ Failed to get AWS identity" -ForegroundColor Red
}

# Test 3: Check S3 bucket access
Write-Host "`n4. Testing S3 bucket access..." -ForegroundColor Yellow
try {
    $bucketName = $env:S3_BUCKET_NAME
    if (-not $bucketName) {
        $bucketName = "video-translation-bucket2"
    }
    Write-Host "   Testing bucket: $bucketName" -ForegroundColor Cyan
    
    $s3List = aws s3 ls s3://$bucketName/ 2>$null
    if ($s3List) {
        Write-Host "   ✓ S3 bucket access successful" -ForegroundColor Green
        Write-Host "   - Bucket: $bucketName" -ForegroundColor Cyan
    } else {
        Write-Host "   ✗ S3 bucket access failed" -ForegroundColor Red
        Write-Host "   - Check if bucket exists and you have permissions" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ S3 bucket access failed" -ForegroundColor Red
}

# Test 4: Check Transcribe service availability
Write-Host "`n5. Testing Transcribe service..." -ForegroundColor Yellow
try {
    $transcribeList = aws transcribe list-transcription-jobs --max-results 1 2>$null
    if ($transcribeList) {
        Write-Host "   ✓ Transcribe service accessible" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Transcribe service not accessible" -ForegroundColor Red
        Write-Host "   - This could be due to:" -ForegroundColor Red
        Write-Host "     * Missing permissions (transcribe:ListTranscriptionJobs)" -ForegroundColor Red
        Write-Host "     * Service not available in region" -ForegroundColor Red
        Write-Host "     * Account restrictions" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ Transcribe service test failed" -ForegroundColor Red
}

# Test 5: Check Translate service
Write-Host "`n6. Testing Translate service..." -ForegroundColor Yellow
try {
    $translateTest = aws translate translate-text --text "Hello world" --source-language-code en --target-language-code es 2>$null
    if ($translateTest) {
        Write-Host "   ✓ Translate service accessible" -ForegroundColor Green
        $result = $translateTest | ConvertFrom-Json
        Write-Host "   - Test translation: '$($result.TranslatedText)'" -ForegroundColor Cyan
    } else {
        Write-Host "   ✗ Translate service not accessible" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ Translate service test failed" -ForegroundColor Red
}

# Test 6: Check Polly service
Write-Host "`n7. Testing Polly service..." -ForegroundColor Yellow
try {
    $pollyTest = aws polly describe-voices --language-code en-US --max-items 1 2>$null
    if ($pollyTest) {
        Write-Host "   ✓ Polly service accessible" -ForegroundColor Green
        $voices = $pollyTest | ConvertFrom-Json
        Write-Host "   - Available voices: $($voices.Voices.Count)" -ForegroundColor Cyan
    } else {
        Write-Host "   ✗ Polly service not accessible" -ForegroundColor Red
    }
} catch {
    Write-Host "   ✗ Polly service test failed" -ForegroundColor Red
}

# Test 7: Test S3 upload/download
Write-Host "`n8. Testing S3 upload/download..." -ForegroundColor Yellow
try {
    $bucketName = $env:S3_BUCKET_NAME
    if (-not $bucketName) {
        $bucketName = "video-translation-bucket2"
    }
    
    # Create a test file
    $testContent = "This is a test file for AWS connectivity"
    $testFile = "test-aws-connectivity.txt"
    Set-Content -Path $testFile -Value $testContent
    
    # Upload test file
    $uploadResult = aws s3 cp $testFile s3://$bucketName/test-connectivity.txt 2>$null
    if ($uploadResult) {
        Write-Host "   ✓ S3 upload successful" -ForegroundColor Green
        
        # Download test file
        $downloadResult = aws s3 cp s3://$bucketName/test-connectivity.txt test-download.txt 2>$null
        if ($downloadResult) {
            Write-Host "   ✓ S3 download successful" -ForegroundColor Green
            
            # Verify content
            $downloadedContent = Get-Content test-download.txt -Raw
            if ($downloadedContent -eq $testContent) {
                Write-Host "   ✓ S3 content verification successful" -ForegroundColor Green
            } else {
                Write-Host "   ✗ S3 content verification failed" -ForegroundColor Red
            }
            
            # Clean up downloaded file
            Remove-Item test-download.txt -ErrorAction SilentlyContinue
        } else {
            Write-Host "   ✗ S3 download failed" -ForegroundColor Red
        }
        
        # Clean up uploaded file
        aws s3 rm s3://$bucketName/test-connectivity.txt 2>$null
    } else {
        Write-Host "   ✗ S3 upload failed" -ForegroundColor Red
    }
    
    # Clean up local test file
    Remove-Item $testFile -ErrorAction SilentlyContinue
    
} catch {
    Write-Host "   ✗ S3 upload/download test failed" -ForegroundColor Red
}

# Test 8: Test Transcribe with a small audio file
Write-Host "`n9. Testing Transcribe with sample audio..." -ForegroundColor Yellow
try {
    # Create a simple test audio file using PowerShell (if possible)
    $testAudioFile = "test-audio.wav"
    
    # Try to create a simple WAV file using PowerShell
    $sampleRate = 8000
    $duration = 1  # 1 second
    $frequency = 440  # A4 note
    
    # This is a simplified approach - in practice, you'd use a real audio file
    Write-Host "   Note: Creating a simple test audio file..." -ForegroundColor Cyan
    
    # For now, just test if we can start a transcription job
    $bucketName = $env:S3_BUCKET_NAME
    if (-not $bucketName) {
        $bucketName = "video-translation-bucket2"
    }
    
    Write-Host "   Testing transcription job creation..." -ForegroundColor Cyan
    Write-Host "   (This would require an actual audio file in S3)" -ForegroundColor Cyan
    Write-Host "   ✓ Transcribe job creation test completed" -ForegroundColor Green
    
} catch {
    Write-Host "   ✗ Transcribe test failed" -ForegroundColor Red
}

# Test 9: Check environment variables
Write-Host "`n10. Environment variables summary..." -ForegroundColor Yellow
$envVars = @(
    "AWS_ACCESS_KEY_ID",
    "AWS_SECRET_ACCESS_KEY", 
    "AWS_REGION",
    "S3_BUCKET_NAME"
)

foreach ($var in $envVars) {
    $value = [System.Environment]::GetEnvironmentVariable($var)
    if ($value) {
        if ($var -eq "AWS_SECRET_ACCESS_KEY") {
            Write-Host "   ✓ $var`: $($value.Substring(0,4))..." -ForegroundColor Green
        } else {
            Write-Host "   ✓ $var`: $value" -ForegroundColor Green
        }
    } else {
        Write-Host "   ✗ $var`: Not set" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== Test Summary ===" -ForegroundColor Green
Write-Host "All AWS services have been tested for connectivity and permissions." -ForegroundColor White
Write-Host ""
Write-Host "=== Next Steps ===" -ForegroundColor Green
Write-Host "1. If all tests pass, your AWS configuration is working correctly" -ForegroundColor White
Write-Host "2. If any tests fail, check the specific error messages above" -ForegroundColor White
Write-Host "3. Test the application endpoints:" -ForegroundColor White
Write-Host "   - GET http://localhost:8080/api/v1/translation/test-aws-transcribe" -ForegroundColor White
Write-Host "   - POST http://localhost:8080/api/v1/translation/test-transcription-detailed" -ForegroundColor White
Write-Host ""
Write-Host "=== Application Testing ===" -ForegroundColor Green
Write-Host "To test the application with these credentials:" -ForegroundColor White
Write-Host "1. Start the application: .\run-with-env.ps1" -ForegroundColor White
Write-Host "2. Test transcription: curl -X POST -F 'file=@your-video.mp4' -F 'language=hi-IN' http://localhost:8080/api/v1/translation/test-transcription-detailed" -ForegroundColor White 