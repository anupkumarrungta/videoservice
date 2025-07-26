# Test Enhanced S3 Verification
# This script tests the enhanced S3 verification process

Write-Host "=== Enhanced S3 Verification Test ===" -ForegroundColor Cyan
Write-Host "Testing enhanced S3 verification with delays and retries..." -ForegroundColor Yellow

# Test 1: Create and upload a test file
Write-Host "`n1. Creating and uploading test file..." -ForegroundColor Cyan
try {
    # Create a test file
    $testContent = "This is a test file for enhanced S3 verification"
    $testFile = "test-enhanced-s3.txt"
    $testContent | Out-File -FilePath $testFile -Encoding UTF8
    
    # Upload to S3
    $s3Key = "transcription/enhanced-test-$(Get-Date -Format 'yyyyMMdd-HHmmss')/test-file.txt"
    $uploadResult = aws s3 cp $testFile "s3://video-translation-bucket2/$s3Key" 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Test file uploaded to S3: $s3Key" -ForegroundColor Green
        
        # Test 2: Immediate verification
        Write-Host "`n2. Testing immediate verification..." -ForegroundColor Cyan
        $headResult = aws s3api head-object --bucket video-translation-bucket2 --key $s3Key 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ File verified immediately" -ForegroundColor Green
        } else {
            Write-Host "✗ File not found immediately" -ForegroundColor Red
        }
        
        # Test 3: Wait and verify multiple times
        Write-Host "`n3. Testing verification with delays..." -ForegroundColor Cyan
        for ($i = 1; $i -le 5; $i++) {
            Write-Host "Verification attempt $i..." -ForegroundColor Yellow
            Start-Sleep -Seconds 2
            
            $verifyResult = aws s3api head-object --bucket video-translation-bucket2 --key $s3Key 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✓ File verified on attempt $i" -ForegroundColor Green
            } else {
                Write-Host "✗ File not found on attempt $i" -ForegroundColor Red
            }
        }
        
        # Test 4: Test file read access
        Write-Host "`n4. Testing file read access..." -ForegroundColor Cyan
        $readResult = aws s3 cp "s3://video-translation-bucket2/$s3Key" test-read-back.txt 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ File read access successful" -ForegroundColor Green
            $readContent = Get-Content test-read-back.txt -Raw
            if ($readContent.Trim() -eq $testContent) {
                Write-Host "✓ File content verified correctly" -ForegroundColor Green
            } else {
                Write-Host "✗ File content mismatch" -ForegroundColor Red
            }
            Remove-Item test-read-back.txt -ErrorAction SilentlyContinue
        } else {
            Write-Host "✗ File read access failed" -ForegroundColor Red
        }
        
        # Clean up
        aws s3 rm "s3://video-translation-bucket2/$s3Key" 2>$null
        
    } else {
        Write-Host "✗ Failed to upload test file" -ForegroundColor Red
        Write-Host $uploadResult
    }
    
    # Clean up local file
    Remove-Item $testFile -ErrorAction SilentlyContinue
    
} catch {
    Write-Host "✗ Error during test: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== Enhanced S3 Verification Test Complete ===" -ForegroundColor Cyan
Write-Host "This test simulates the enhanced verification process with delays and retries." -ForegroundColor Green 