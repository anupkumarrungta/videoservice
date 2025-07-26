# Debug AWS Transcribe S3 404 Issue
# This script helps identify the exact cause of the S3 404 error

Write-Host "=== AWS Transcribe S3 404 Debug Script ===" -ForegroundColor Cyan
Write-Host "Debugging the S3 404 error in AWS Transcribe..." -ForegroundColor Yellow

# Test 1: Check AWS region configuration
Write-Host "`n1. Checking AWS region configuration..." -ForegroundColor Cyan
try {
    $region = aws configure get region
    Write-Host "✓ AWS CLI region: $region" -ForegroundColor Green
    
    $envRegion = $env:AWS_REGION
    Write-Host "✓ Environment AWS_REGION: $envRegion" -ForegroundColor Green
    
    if ($region -ne $envRegion) {
        Write-Host "⚠ WARNING: Region mismatch between CLI and environment" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ Failed to check region configuration" -ForegroundColor Red
}

# Test 2: Check S3 bucket region
Write-Host "`n2. Checking S3 bucket region..." -ForegroundColor Cyan
try {
    $bucketRegion = aws s3api get-bucket-location --bucket video-translation-bucket2 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ S3 bucket region: $bucketRegion" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to get bucket region" -ForegroundColor Red
        Write-Host $bucketRegion
    }
} catch {
    Write-Host "✗ Failed to check S3 bucket region" -ForegroundColor Red
}

# Test 3: Create a test file with specific naming
Write-Host "`n3. Creating test file with application-like naming..." -ForegroundColor Cyan
try {
    # Create a test audio file
    $testAudioFile = "test-debug-audio.mp3"
    
    # Use ffmpeg to create a 2-second silent MP3 file
    & "C:\ffmpeg\bin\ffmpeg.exe" -f lavfi -i anullsrc=channel_layout=stereo:sample_rate=44100 -t 2 -c:a mp3 $testAudioFile -y 2>$null
    
    if (Test-Path $testAudioFile) {
        Write-Host "✓ Test audio file created: $testAudioFile" -ForegroundColor Green
        
        # Upload to S3 with application-like naming
        $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
        $uuid = [System.Guid]::NewGuid().ToString()
        $s3Key = "transcription/$uuid/test-debug-audio.mp3"
        
        Write-Host "Uploading to S3 key: $s3Key" -ForegroundColor Yellow
        $uploadResult = aws s3 cp $testAudioFile "s3://video-translation-bucket2/$s3Key" 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ Test file uploaded successfully" -ForegroundColor Green
            
            # Test 4: Verify file exists immediately
            Write-Host "`n4. Verifying file exists immediately..." -ForegroundColor Cyan
            $headResult = aws s3api head-object --bucket video-translation-bucket2 --key $s3Key 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✓ File verified immediately" -ForegroundColor Green
                Write-Host "File details:" -ForegroundColor White
                Write-Host $headResult
            } else {
                Write-Host "✗ File not found immediately" -ForegroundColor Red
                Write-Host $headResult
            }
            
            # Test 5: Wait and verify multiple times
            Write-Host "`n5. Testing verification with delays..." -ForegroundColor Cyan
            for ($i = 1; $i -le 10; $i++) {
                Write-Host "Verification attempt $i..." -ForegroundColor Yellow
                Start-Sleep -Seconds 1
                
                $verifyResult = aws s3api head-object --bucket video-translation-bucket2 --key $s3Key 2>&1
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "✓ File verified on attempt $i" -ForegroundColor Green
                } else {
                    Write-Host "✗ File not found on attempt $i" -ForegroundColor Red
                }
            }
            
            # Test 6: Test transcription job with this file
            Write-Host "`n6. Testing transcription job with the file..." -ForegroundColor Cyan
            $s3Uri = "s3://video-translation-bucket2/$s3Key"
            $jobName = "debug-test-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
            
            Write-Host "S3 URI: $s3Uri" -ForegroundColor White
            Write-Host "Job name: $jobName" -ForegroundColor White
            
            $transcribeResult = aws transcribe start-transcription-job `
                --transcription-job-name $jobName `
                --media MediaFileUri=$s3Uri `
                --language-code en-US `
                --output-bucket-name video-translation-bucket2 `
                --output-key "transcription-results/$jobName.json" 2>&1
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✓ Transcription job started successfully" -ForegroundColor Green
                Write-Host "Job details:" -ForegroundColor White
                Write-Host $transcribeResult
                
                # Wait and check job status
                Write-Host "`nWaiting for job completion..." -ForegroundColor Yellow
                Start-Sleep -Seconds 10
                
                $statusResult = aws transcribe get-transcription-job --transcription-job-name $jobName 2>&1
                Write-Host "Job status:" -ForegroundColor White
                Write-Host $statusResult
                
                # Clean up the test job
                Write-Host "`nCleaning up test job..." -ForegroundColor Yellow
                aws transcribe delete-transcription-job --transcription-job-name $jobName 2>$null
                
            } else {
                Write-Host "✗ Failed to start transcription job" -ForegroundColor Red
                Write-Host $transcribeResult
            }
            
            # Clean up S3 file
            aws s3 rm "s3://video-translation-bucket2/$s3Key" 2>$null
            
        } else {
            Write-Host "✗ Failed to upload test file" -ForegroundColor Red
            Write-Host $uploadResult
        }
        
        # Clean up local file
        Remove-Item $testAudioFile -ErrorAction SilentlyContinue
        
    } else {
        Write-Host "✗ Failed to create test audio file" -ForegroundColor Red
    }
    
} catch {
    Write-Host "✗ Error during test: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 7: Check existing transcription jobs
Write-Host "`n7. Checking existing transcription jobs..." -ForegroundColor Cyan
try {
    $jobsResult = aws transcribe list-transcription-jobs --max-items 5 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Retrieved transcription jobs" -ForegroundColor Green
        Write-Host "Recent jobs:" -ForegroundColor White
        Write-Host $jobsResult
    } else {
        Write-Host "✗ Failed to retrieve transcription jobs" -ForegroundColor Red
        Write-Host $jobsResult
    }
} catch {
    Write-Host "✗ Failed to check transcription jobs" -ForegroundColor Red
}

Write-Host "`n=== Debug Complete ===" -ForegroundColor Cyan
Write-Host "This test helps identify the exact cause of the S3 404 error." -ForegroundColor Green
Write-Host "Check the results above for any failures or warnings." -ForegroundColor Yellow 