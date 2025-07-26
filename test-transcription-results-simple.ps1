# Simple Transcription Results Test
# This script tests transcription result file creation and access

Write-Host "=== Simple Transcription Results Test ===" -ForegroundColor Cyan
Write-Host "Testing transcription result file creation and access..." -ForegroundColor Yellow

# Test 1: Check existing transcription result files
Write-Host "`n1. Checking existing transcription result files..." -ForegroundColor Cyan
try {
    $resultsList = aws s3 ls s3://video-translation-bucket2/transcription-results/ 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Transcription results folder accessible" -ForegroundColor Green
        Write-Host "Recent result files:" -ForegroundColor White
        Write-Host $resultsList
    } else {
        Write-Host "✗ Failed to access transcription results folder" -ForegroundColor Red
        Write-Host $resultsList
    }
} catch {
    Write-Host "✗ Failed to check transcription results" -ForegroundColor Red
}

# Test 2: Create a test transcription job and check result
Write-Host "`n2. Creating test transcription job..." -ForegroundColor Cyan
try {
    # Create a test audio file
    $testAudioFile = "test-simple-audio.mp3"
    
    # Use ffmpeg to create a 1-second silent MP3 file
    & "C:\ffmpeg\bin\ffmpeg.exe" -f lavfi -i anullsrc=channel_layout=stereo:sample_rate=44100 -t 1 -c:a mp3 $testAudioFile -y 2>$null
    
    if (Test-Path $testAudioFile) {
        Write-Host "✓ Test audio file created: $testAudioFile" -ForegroundColor Green
        
        # Upload to S3
        $s3Key = "transcription/test-simple-$(Get-Date -Format 'yyyyMMdd-HHmmss')/test-audio.mp3"
        $uploadResult = aws s3 cp $testAudioFile "s3://video-translation-bucket2/$s3Key" 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ Test file uploaded to S3: $s3Key" -ForegroundColor Green
            
            # Start transcription job
            $s3Uri = "s3://video-translation-bucket2/$s3Key"
            $jobName = "test-simple-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
            
            Write-Host "Starting transcription job: $jobName" -ForegroundColor Yellow
            $transcribeResult = aws transcribe start-transcription-job `
                --transcription-job-name $jobName `
                --media MediaFileUri=$s3Uri `
                --language-code en-US `
                --output-bucket-name video-translation-bucket2 `
                --output-key "transcription-results/$jobName.json" 2>&1
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✓ Transcription job started successfully" -ForegroundColor Green
                
                # Wait for job completion
                Write-Host "Waiting for job completion..." -ForegroundColor Yellow
                $completed = $false
                $attempts = 0
                $maxAttempts = 30
                
                while (-not $completed -and $attempts -lt $maxAttempts) {
                    Start-Sleep -Seconds 2
                    $attempts++
                    
                    $statusResult = aws transcribe get-transcription-job --transcription-job-name $jobName 2>&1
                    if ($LASTEXITCODE -eq 0) {
                        if ($statusResult -like "*COMPLETED*") {
                            Write-Host "✓ Job completed on attempt $attempts" -ForegroundColor Green
                            $completed = $true
                            
                            # Test 3: Check if result file exists
                            Write-Host "`n3. Checking transcription result file..." -ForegroundColor Cyan
                            $resultKey = "transcription-results/$jobName.json"
                            
                            # Wait a moment for file to be written
                            Start-Sleep -Seconds 3
                            
                            $headResult = aws s3api head-object --bucket video-translation-bucket2 --key $resultKey 2>&1
                            if ($LASTEXITCODE -eq 0) {
                                Write-Host "✓ Result file exists" -ForegroundColor Green
                                Write-Host "Result file details:" -ForegroundColor White
                                Write-Host $headResult
                                
                                # Test 4: Download and read result file
                                Write-Host "`n4. Downloading and reading result file..." -ForegroundColor Cyan
                                $downloadResult = aws s3 cp "s3://video-translation-bucket2/$resultKey" test-simple-result.json 2>&1
                                if ($LASTEXITCODE -eq 0) {
                                    Write-Host "✓ Result file downloaded" -ForegroundColor Green
                                    $resultContent = Get-Content test-simple-result.json -Raw
                                    Write-Host "Result content (first 200 chars):" -ForegroundColor White
                                    Write-Host $resultContent.Substring(0, [Math]::Min(200, $resultContent.Length))
                                    
                                    # Clean up local file
                                    Remove-Item test-simple-result.json -ErrorAction SilentlyContinue
                                } else {
                                    Write-Host "✗ Failed to download result file" -ForegroundColor Red
                                    Write-Host $downloadResult
                                }
                            } else {
                                Write-Host "✗ Result file not found" -ForegroundColor Red
                                Write-Host $headResult
                            }
                            
                        } elseif ($statusResult -like "*FAILED*") {
                            Write-Host "✗ Job failed" -ForegroundColor Red
                            Write-Host $statusResult
                            break
                        } else {
                            Write-Host "Job still in progress... (attempt $attempts)" -ForegroundColor Yellow
                        }
                    } else {
                        Write-Host "✗ Failed to get job status" -ForegroundColor Red
                        Write-Host $statusResult
                        break
                    }
                }
                
                if (-not $completed) {
                    Write-Host "✗ Job did not complete within expected time" -ForegroundColor Red
                }
                
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

Write-Host "`n=== Simple Transcription Results Test Complete ===" -ForegroundColor Cyan
Write-Host "This test verifies that transcription result files are created and accessible." -ForegroundColor Green 