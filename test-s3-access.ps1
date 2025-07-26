# Test S3 Access Script
# This script tests S3 bucket access and file operations

Write-Host "=== S3 Access Test Script ===" -ForegroundColor Cyan
Write-Host "Testing S3 bucket access and file operations..." -ForegroundColor Yellow

# Test 1: Check if bucket exists and is accessible
Write-Host "`n1. Testing S3 bucket access..." -ForegroundColor Cyan
try {
    $bucketTest = aws s3 ls s3://video-translation-bucket2 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ S3 bucket is accessible" -ForegroundColor Green
        Write-Host "Bucket contents:" -ForegroundColor White
        Write-Host $bucketTest
    } else {
        Write-Host "✗ S3 bucket access failed" -ForegroundColor Red
        Write-Host $bucketTest
    }
} catch {
    Write-Host "✗ Failed to test S3 bucket access" -ForegroundColor Red
}

# Test 2: Test file upload and download
Write-Host "`n2. Testing S3 file operations..." -ForegroundColor Cyan
try {
    # Create a test file
    $testContent = "This is a test file for S3 access verification"
    $testFile = "test-s3-access.txt"
    $testContent | Out-File -FilePath $testFile -Encoding UTF8
    
    # Upload test file
    Write-Host "Uploading test file..." -ForegroundColor Yellow
    $uploadResult = aws s3 cp $testFile s3://video-translation-bucket2/test-s3-access.txt 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Test file uploaded successfully" -ForegroundColor Green
        
        # Download test file
        Write-Host "Downloading test file..." -ForegroundColor Yellow
        $downloadResult = aws s3 cp s3://video-translation-bucket2/test-s3-access.txt test-s3-download.txt 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ Test file downloaded successfully" -ForegroundColor Green
            
            # Verify content
            $downloadedContent = Get-Content test-s3-download.txt -Raw
            if ($downloadedContent.Trim() -eq $testContent) {
                Write-Host "✓ File content verified correctly" -ForegroundColor Green
            } else {
                Write-Host "✗ File content mismatch" -ForegroundColor Red
            }
        } else {
            Write-Host "✗ Test file download failed" -ForegroundColor Red
            Write-Host $downloadResult
        }
        
        # Clean up
        aws s3 rm s3://video-translation-bucket2/test-s3-access.txt 2>&1 | Out-Null
        Remove-Item test-s3-download.txt -ErrorAction SilentlyContinue
    } else {
        Write-Host "✗ Test file upload failed" -ForegroundColor Red
        Write-Host $uploadResult
    }
    
    # Clean up local test file
    Remove-Item $testFile -ErrorAction SilentlyContinue
    
} catch {
    Write-Host "✗ Failed to test S3 file operations" -ForegroundColor Red
}

# Test 3: Check transcription folder structure
Write-Host "`n3. Checking transcription folder structure..." -ForegroundColor Cyan
try {
    $transcriptionFolders = aws s3 ls s3://video-translation-bucket2/transcription/ 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Transcription folder exists" -ForegroundColor Green
        Write-Host "Transcription folder contents:" -ForegroundColor White
        Write-Host $transcriptionFolders
    } else {
        Write-Host "✗ Transcription folder not found or not accessible" -ForegroundColor Red
        Write-Host $transcriptionFolders
    }
} catch {
    Write-Host "✗ Failed to check transcription folder" -ForegroundColor Red
}

# Test 4: Check transcription-results folder
Write-Host "`n4. Checking transcription-results folder..." -ForegroundColor Cyan
try {
    $resultsFolders = aws s3 ls s3://video-translation-bucket2/transcription-results/ 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Transcription-results folder exists" -ForegroundColor Green
        Write-Host "Results folder contents:" -ForegroundColor White
        Write-Host $resultsFolders
    } else {
        Write-Host "✗ Transcription-results folder not found or not accessible" -ForegroundColor Red
        Write-Host $resultsFolders
    }
} catch {
    Write-Host "✗ Failed to check transcription-results folder" -ForegroundColor Red
}

Write-Host "`n=== S3 Access Test Complete ===" -ForegroundColor Cyan
Write-Host "If all tests pass, S3 access should work correctly." -ForegroundColor Green
Write-Host "If any tests fail, check your AWS credentials and bucket permissions." -ForegroundColor Yellow 