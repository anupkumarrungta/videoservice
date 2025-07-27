Write-Host "Testing FFmpeg availability..." -ForegroundColor Green

# Test FFmpeg
try {
    $ffmpegOutput = & "C:\ffmpeg\bin\ffmpeg.exe" -version 2>&1 | Select-Object -First 1
    Write-Host "FFmpeg: $ffmpegOutput" -ForegroundColor Green
} catch {
    Write-Host "FFmpeg not found" -ForegroundColor Red
    exit 1
}

# Test FFprobe
try {
    $ffprobeOutput = & "C:\ffmpeg\bin\ffprobe.exe" -version 2>&1 | Select-Object -First 1
    Write-Host "FFprobe: $ffprobeOutput" -ForegroundColor Green
} catch {
    Write-Host "FFprobe not found" -ForegroundColor Red
    exit 1
}

# Create a simple test audio file
Write-Host "Creating test audio file..." -ForegroundColor Green
$testFile = "test_audio.mp3"

try {
    & "C:\ffmpeg\bin\ffmpeg.exe" -f lavfi -i "anullsrc=channel_layout=mono:sample_rate=16000" -t 5 -acodec libmp3lame -ar 16000 -ac 1 -b:a 128k -y $testFile 2>$null
    
    if (Test-Path $testFile) {
        $size = (Get-Item $testFile).Length
        Write-Host "Test audio created: $testFile ($size bytes)" -ForegroundColor Green
        
        # Test getting duration
        $duration = & "C:\ffmpeg\bin\ffprobe.exe" -v quiet -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 $testFile 2>$null
        if ($duration) {
            Write-Host "Duration: $duration seconds" -ForegroundColor Green
        }
        
        # Clean up
        Remove-Item $testFile
        Write-Host "Test completed successfully" -ForegroundColor Green
    } else {
        Write-Host "Failed to create test audio file" -ForegroundColor Red
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
} 