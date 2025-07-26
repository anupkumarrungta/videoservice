# Test AWS Transcribe S3 URI Format
# This script tests the exact S3 URI format that AWS Transcribe expects

Write-Host "=== AWS Transcribe S3 URI Test ===" -ForegroundColor Cyan
Write-Host "Testing S3 URI format for AWS Transcribe..." -ForegroundColor Yellow

# Test 1: Create a test audio file and upload it
Write-Host "`n1. Creating and uploading test audio file..." -ForegroundColor Cyan
try {
    # Create a simple test audio file (1 second of silence)
    $testAudioFile = "test-audio.mp3"
    
    # Use ffmpeg to create a 1-second silent MP3 file
    & "C:\ffmpeg\bin\ffmpeg.exe" -f lavfi -i anullsrc=channel_layout=stereo:sample_rate=44100 -t 1 -c:a mp3 $testAudioFile -y 2>$null
    
    if (Test-Path $testAudioFile) {
        Write-Host "✓ Test audio file created: $testAudioFile" -ForegroundColor Green
        
        # Upload to S3
        $s3Key = "transcription/test-uri-$(Get-Date -Format 'yyyyMMdd-HHmmss')/test-audio.mp3"
        $uploadResult = aws s3 cp $testAudioFile "s3://video-translation-bucket2/$s3Key" 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ Test audio file uploaded to S3: $s3Key" -ForegroundColor Green
            
            # Test 2: Verify the file exists in S3
            Write-Host "`n2. Verifying file exists in S3..." -ForegroundColor Cyan
            $headResult = aws s3api head-object --bucket video-translation-bucket2 --key $s3Key 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✓ File exists in S3" -ForegroundColor Green
                Write-Host "File details:" -ForegroundColor White
                Write-Host $headResult
                
                # Test 3: Test the S3 URI format
                Write-Host "`n3. Testing S3 URI format..." -ForegroundColor Cyan
                $s3Uri = "s3://video-translation-bucket2/$s3Key"
                Write-Host "S3 URI: $s3Uri" -ForegroundColor White
                
                # Test 4: Try to start a transcription job with this URI
                Write-Host "`n4. Testing transcription job creation..." -ForegroundColor Cyan
                $jobName = "test-uri-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
                
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
                    
                    # Wait a moment and check job status
                    Start-Sleep -Seconds 5
                    $statusResult = aws transcribe get-transcription-job --transcription-job-name $jobName 2>&1
                    Write-Host "`nJob status:" -ForegroundColor White
                    Write-Host $statusResult
                    
                    # Clean up the test job
                    Write-Host "`nCleaning up test job..." -ForegroundColor Yellow
                    aws transcribe delete-transcription-job --transcription-job-name $jobName 2>$null
                    
                } else {
                    Write-Host "✗ Failed to start transcription job" -ForegroundColor Red
                    Write-Host $transcribeResult
                }
                
            } else {
                Write-Host "✗ File does not exist in S3" -ForegroundColor Red
                Write-Host $headResult
            }
            
            # Clean up S3 file
            aws s3 rm "s3://video-translation-bucket2/$s3Key" 2>$null
            
        } else {
            Write-Host "✗ Failed to upload test audio file" -ForegroundColor Red
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

Write-Host "`n=== S3 URI Test Complete ===" -ForegroundColor Cyan
Write-Host "This test verifies the exact S3 URI format that AWS Transcribe expects." -ForegroundColor Green 