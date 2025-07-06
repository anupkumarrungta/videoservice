# FFmpeg Installation Script for Windows
# This script downloads and installs FFmpeg on Windows

Write-Host "Starting FFmpeg installation..." -ForegroundColor Green

# Create installation directory
$installDir = "C:\ffmpeg"
$binDir = "$installDir\bin"

if (Test-Path $installDir) {
    Write-Host "FFmpeg directory already exists. Removing old installation..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force $installDir
}

Write-Host "Creating installation directory: $installDir" -ForegroundColor Green
New-Item -ItemType Directory -Path $installDir -Force | Out-Null
New-Item -ItemType Directory -Path $binDir -Force | Out-Null

# Download FFmpeg
$ffmpegUrl = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip"
$zipFile = "$env:TEMP\ffmpeg.zip"

Write-Host "Downloading FFmpeg from: $ffmpegUrl" -ForegroundColor Green
try {
    Invoke-WebRequest -Uri $ffmpegUrl -OutFile $zipFile -UseBasicParsing
    Write-Host "Download completed successfully" -ForegroundColor Green
} catch {
    Write-Host "Failed to download FFmpeg: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Extract FFmpeg
Write-Host "Extracting FFmpeg..." -ForegroundColor Green
try {
    Expand-Archive -Path $zipFile -DestinationPath "$env:TEMP\ffmpeg-extract" -Force
    Write-Host "Extraction completed" -ForegroundColor Green
} catch {
    Write-Host "Failed to extract FFmpeg: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Find the extracted directory (it has a random name)
$extractedDir = Get-ChildItem -Path "$env:TEMP\ffmpeg-extract" -Directory | Select-Object -First 1
if (-not $extractedDir) {
    Write-Host "Could not find extracted FFmpeg directory" -ForegroundColor Red
    exit 1
}

# Copy FFmpeg files to installation directory
Write-Host "Installing FFmpeg to: $installDir" -ForegroundColor Green
try {
    Copy-Item -Path "$($extractedDir.FullName)\bin\*" -Destination $binDir -Recurse -Force
    Write-Host "Installation completed successfully" -ForegroundColor Green
} catch {
    Write-Host "Failed to copy FFmpeg files: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Clean up temporary files
Write-Host "Cleaning up temporary files..." -ForegroundColor Green
Remove-Item -Path $zipFile -Force -ErrorAction SilentlyContinue
Remove-Item -Path "$env:TEMP\ffmpeg-extract" -Recurse -Force -ErrorAction SilentlyContinue

# Verify installation
Write-Host "Verifying installation..." -ForegroundColor Green
$ffmpegExe = "$binDir\ffmpeg.exe"
$ffprobeExe = "$binDir\ffprobe.exe"

if (Test-Path $ffmpegExe) {
    Write-Host "✓ FFmpeg installed successfully: $ffmpegExe" -ForegroundColor Green
} else {
    Write-Host "✗ FFmpeg installation failed" -ForegroundColor Red
    exit 1
}

if (Test-Path $ffprobeExe) {
    Write-Host "✓ FFprobe installed successfully: $ffprobeExe" -ForegroundColor Green
} else {
    Write-Host "✗ FFprobe installation failed" -ForegroundColor Red
    exit 1
}

# Test FFmpeg
Write-Host 'Testing FFmpeg...' -ForegroundColor Green
try {
    $ffmpegVersion = & $ffmpegExe -version | Select-Object -First 1
    Write-Host ('FFmpeg version: ' + $ffmpegVersion) -ForegroundColor Green
} catch {
    Write-Host ('FFmpeg test failed: ' + $_.Exception.Message) -ForegroundColor Red
}

# Add to PATH (optional - user can do this manually)
Write-Host ''
Write-Host 'Installation completed successfully!' -ForegroundColor Green
Write-Host ('FFmpeg is installed at: ' + $binDir) -ForegroundColor Cyan
Write-Host ''
Write-Host 'To add FFmpeg to your system PATH:' -ForegroundColor Yellow
Write-Host '1. Open System Properties (Win + Pause/Break)' -ForegroundColor White
Write-Host '2. Click ''Advanced system settings''' -ForegroundColor White
Write-Host '3. Click ''Environment Variables''' -ForegroundColor White
Write-Host '4. Under ''System variables'', find ''Path'' and click ''Edit''' -ForegroundColor White
Write-Host '5. Click ''New'' and add: ' -NoNewline; Write-Host $binDir -ForegroundColor Cyan
Write-Host '6. Click ''OK'' on all dialogs' -ForegroundColor White
Write-Host '7. Restart your terminal/IDE' -ForegroundColor White
Write-Host ''
Write-Host 'Or you can set the paths in your .env file:' -ForegroundColor Yellow
Write-Host ('FFMPEG_PATH=' + $binDir + '\ffmpeg.exe') -ForegroundColor Cyan
Write-Host ('FFPROBE_PATH=' + $binDir + '\ffprobe.exe') -ForegroundColor Cyan 