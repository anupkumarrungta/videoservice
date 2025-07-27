# Test Audio Chunking Process
Write-Host "=== Audio Chunking Debug Script ===" -ForegroundColor Green

# Check if FFmpeg is available
Write-Host "1. Checking FFmpeg installation..." -ForegroundColor Yellow
try {
    $ffmpegVersion = & "C:\ffmpeg\bin\ffmpeg.exe" -version 2>&1 | Select-Object -First 1
    Write-Host "   FFmpeg found: $ffmpegVersion" -ForegroundColor Green
} catch {
    Write-Host "   ERROR: FFmpeg not found" -ForegroundColor Red
    exit 1
}

# Look for video files in uploads directory
Write-Host "2. Looking for video files..." -ForegroundColor Yellow
$uploadsDir = "uploads"
if (Test-Path $uploadsDir) {
    $videoFiles = Get-ChildItem -Path $uploadsDir -Filter "*.mp4" | Where-Object { $_.Length -gt 1MB }
    if ($videoFiles.Count -gt 0) {
        Write-Host "   Found $($videoFiles.Count) video files" -ForegroundColor Green
        foreach ($file in $videoFiles) {
            $sizeMB = [math]::Round($file.Length / 1048576, 2)
            Write-Host "   - $($file.Name) ($sizeMB MB)" -ForegroundColor Cyan
        }
    } else {
        Write-Host "   No video files found" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "   Uploads directory not found" -ForegroundColor Red
    exit 1
}

# Test with the first video file
$testVideo = $videoFiles[0]
Write-Host "3. Testing with video: $($testVideo.Name)" -ForegroundColor Yellow

# Create test directory
$testDir = "test-audio-chunking"
if (Test-Path $testDir) {
    Remove-Item -Path $testDir -Recurse -Force
}
New-Item -ItemType Directory -Path $testDir | Out-Null

# Step 1: Check if video has audio
Write-Host "   Step 1: Checking if video has audio..." -ForegroundColor Cyan
try {
    $audioCheck = & "C:\ffmpeg\bin\ffprobe.exe" -v quiet -show_entries stream=codec_type -select_streams a -of csv=p=0 $testVideo.FullName
    if ($audioCheck.Trim() -eq "audio") {
        Write-Host "   ✓ Video has audio stream" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Video has no audio stream" -ForegroundColor Red
        Write-Host "   This is the root cause!" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "   ✗ Error checking audio stream" -ForegroundColor Red
    exit 1
}

# Step 2: Get video properties
Write-Host "   Step 2: Getting video properties..." -ForegroundColor Cyan
try {
    $videoInfo = & "C:\ffmpeg\bin\ffprobe.exe" -v quiet -show_entries format=duration,size -show_streams -of json $testVideo.FullName | ConvertFrom-Json
    $duration = [double]$videoInfo.format.duration
    $size = [long]$videoInfo.format.size
    $durationRounded = [math]::Round($duration, 2)
    $sizeMB = [math]::Round($size / 1048576, 2)
    Write-Host "   ✓ Video duration: $durationRounded seconds" -ForegroundColor Green
    Write-Host "   ✓ Video size: $sizeMB MB" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Error getting video properties" -ForegroundColor Red
    exit 1
}

# Step 3: Extract audio
Write-Host "   Step 3: Extracting audio..." -ForegroundColor Cyan
$audioFile = Join-Path $testDir "extracted_audio.mp3"
try {
    $ffmpegArgs = @(
        "-i", $testVideo.FullName,
        "-vn",
        "-acodec", "libmp3lame",
        "-ar", "16000",
        "-ac", "1",
        "-b:a", "128k",
        "-y",
        $audioFile
    )
    
    $process = Start-Process -FilePath "C:\ffmpeg\bin\ffmpeg.exe" -ArgumentList $ffmpegArgs -Wait -PassThru -NoNewWindow
    if ($process.ExitCode -eq 0 -and (Test-Path $audioFile)) {
        $audioSize = (Get-Item $audioFile).Length
        $audioSizeKB = [math]::Round($audioSize / 1024, 2)
        Write-Host "   ✓ Audio extracted: $audioSizeKB KB" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Audio extraction failed" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "   ✗ Error extracting audio" -ForegroundColor Red
    exit 1
}

# Step 4: Validate extracted audio
Write-Host "   Step 4: Validating extracted audio..." -ForegroundColor Cyan
try {
    $audioInfo = & "C:\ffmpeg\bin\ffprobe.exe" -v quiet -show_entries format=duration,size -show_streams -of json $audioFile | ConvertFrom-Json
    $audioDuration = [double]$audioInfo.format.duration
    $audioSize = [long]$audioInfo.format.size
    
    if ($audioDuration -lt 1.0) {
        Write-Host "   ✗ Audio duration too short: $audioDuration seconds" -ForegroundColor Red
        exit 1
    }
    
    if ($audioSize -lt 1024) {
        Write-Host "   ✗ Audio file too small: $audioSize bytes" -ForegroundColor Red
        exit 1
    }
    
    $audioDurationRounded = [math]::Round($audioDuration, 2)
    $audioSizeKB = [math]::Round($audioSize / 1024, 2)
    Write-Host "   ✓ Audio duration: $audioDurationRounded seconds" -ForegroundColor Green
    Write-Host "   ✓ Audio size: $audioSizeKB KB" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Error validating audio" -ForegroundColor Red
    exit 1
}

# Step 5: Test audio chunking
Write-Host "   Step 5: Testing audio chunking..." -ForegroundColor Cyan
$chunkDuration = 30
$numChunks = [math]::Ceiling($audioDuration / $chunkDuration)
Write-Host "   Will create $numChunks chunks of $chunkDuration seconds each" -ForegroundColor Cyan

$validChunks = 0
for ($i = 0; $i -lt $numChunks; $i++) {
    $startTime = $i * $chunkDuration
    $endTime = [math]::Min(($i + 1) * $chunkDuration, $audioDuration)
    $chunkDurationActual = $endTime - $startTime
    
    if ($chunkDurationActual -lt 0.5) {
        $durationRounded = [math]::Round($chunkDurationActual, 2)
        Write-Host "   Skipping chunk $i - duration too short: $durationRounded s" -ForegroundColor Yellow
        continue
    }
    
    $chunkFileName = "chunk_" + $i.ToString("000") + ".mp3"
    $chunkFile = Join-Path $testDir $chunkFileName
    
    try {
        $ffmpegArgs = @(
            "-i", $audioFile,
            "-ss", [string]$startTime,
            "-t", [string]$chunkDurationActual,
            "-acodec", "libmp3lame",
            "-ar", "16000",
            "-ac", "1",
            "-b:a", "128k",
            "-y",
            $chunkFile
        )
        
        $process = Start-Process -FilePath "C:\ffmpeg\bin\ffmpeg.exe" -ArgumentList $ffmpegArgs -Wait -PassThru -NoNewWindow
        if ($process.ExitCode -eq 0 -and (Test-Path $chunkFile)) {
            $chunkSize = (Get-Item $chunkFile).Length
            if ($chunkSize -gt 1024) {
                $validChunks++
                $chunkSizeKB = [math]::Round($chunkSize / 1024, 2)
                Write-Host "   ✓ Chunk $i created: $chunkSizeKB KB" -ForegroundColor Green
            } else {
                Write-Host "   ✗ Chunk $i too small: $chunkSize bytes" -ForegroundColor Red
            }
        } else {
            Write-Host "   ✗ Chunk $i creation failed" -ForegroundColor Red
        }
    } catch {
        Write-Host "   ✗ Error creating chunk $i" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== Test Results ===" -ForegroundColor Green
Write-Host "Valid chunks created: $validChunks out of $numChunks" -ForegroundColor Cyan

if ($validChunks -eq 0) {
    Write-Host "✗ No valid chunks created - this explains the application error!" -ForegroundColor Red
} else {
    Write-Host "✓ Audio chunking is working correctly" -ForegroundColor Green
}

# Cleanup
Write-Host ""
Write-Host "Cleaning up test files..." -ForegroundColor Yellow
if (Test-Path $testDir) {
    Remove-Item -Path $testDir -Recurse -Force
}

Write-Host "Test completed!" -ForegroundColor Green 