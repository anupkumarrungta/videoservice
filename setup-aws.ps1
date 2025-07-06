# AWS CLI Setup Script for Windows
# This script helps install and configure AWS CLI

Write-Host "AWS CLI Setup for Video Translation Service" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green

# Check if AWS CLI is already installed
try {
    $awsVersion = aws --version 2>$null
    if ($awsVersion) {
        Write-Host "AWS CLI is already installed: $awsVersion" -ForegroundColor Green
        $awsInstalled = $true
    }
} catch {
    $awsInstalled = $false
}

if (-not $awsInstalled) {
    Write-Host "Installing AWS CLI..." -ForegroundColor Yellow
    
    # Download AWS CLI
    $downloadUrl = "https://awscli.amazonaws.com/AWSCLIV2.msi"
    $installerPath = "$env:TEMP\AWSCLIV2.msi"
    
    try {
        Write-Host "Downloading AWS CLI installer..." -ForegroundColor Yellow
        Invoke-WebRequest -Uri $downloadUrl -OutFile $installerPath
        
        Write-Host "Installing AWS CLI..." -ForegroundColor Yellow
        Start-Process msiexec.exe -Wait -ArgumentList "/I $installerPath /quiet"
        
        # Refresh environment variables
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
        
        Write-Host "AWS CLI installed successfully!" -ForegroundColor Green
    } catch {
        Write-Host "Failed to install AWS CLI automatically" -ForegroundColor Red
        Write-Host "Please install manually from: https://aws.amazon.com/cli/" -ForegroundColor Yellow
        exit 1
    }
}

# Check if AWS CLI is now available
try {
    $awsVersion = aws --version 2>$null
    if ($awsVersion) {
        Write-Host "AWS CLI is working: $awsVersion" -ForegroundColor Green
    } else {
        throw "AWS CLI not found"
    }
} catch {
    Write-Host "AWS CLI installation failed. Please restart your terminal and try again." -ForegroundColor Red
    exit 1
}

# Configure AWS CLI
Write-Host "`nConfiguring AWS CLI..." -ForegroundColor Yellow
Write-Host "You will be prompted for your AWS credentials." -ForegroundColor Cyan
Write-Host "If you don't have them yet, please:" -ForegroundColor Cyan
Write-Host "1. Go to AWS Console -> IAM -> Users -> Create User" -ForegroundColor Cyan
Write-Host "2. Attach the VideoTranslationServicePolicy" -ForegroundColor Cyan
Write-Host "3. Download the access keys" -ForegroundColor Cyan
Write-Host ""

$configure = Read-Host "Do you want to configure AWS CLI now? (y/n)"
if ($configure -eq 'y' -or $configure -eq 'Y') {
    aws configure
} else {
    Write-Host "You can configure AWS CLI later by running: aws configure" -ForegroundColor Yellow
}

# Test AWS services
Write-Host "`nTesting AWS Services..." -ForegroundColor Yellow

# Test AWS CLI configuration
try {
    $stsResult = aws sts get-caller-identity 2>$null
    if ($stsResult) {
        Write-Host "AWS CLI configured successfully!" -ForegroundColor Green
        $accountInfo = $stsResult | ConvertFrom-Json
        Write-Host "Account ID: $($accountInfo.Account)" -ForegroundColor Cyan
    } else {
        Write-Host "AWS CLI not configured or credentials invalid" -ForegroundColor Yellow
    }
} catch {
    Write-Host "AWS CLI not configured or credentials invalid" -ForegroundColor Yellow
}

# Create test script
$testScript = @'
# AWS Service Test Script
Write-Host "Testing AWS Services..." -ForegroundColor Green

# Test S3 (replace with your bucket name)
Write-Host "Testing S3 access..." -ForegroundColor Yellow
aws s3 ls s3://your-bucket-name --region us-east-1

# Test Translate
Write-Host "Testing Translate service..." -ForegroundColor Yellow
aws translate translate-text --text "Hello world" --source-language-code en --target-language-code es --region us-east-1

# Test Polly
Write-Host "Testing Polly service..." -ForegroundColor Yellow
aws polly synthesize-speech --text "Hello world" --voice-id Joanna --output-format mp3 --region us-east-1 test-output.mp3

Write-Host "AWS service tests completed!" -ForegroundColor Green
'@

Set-Content -Path "test-aws-services.ps1" -Value $testScript

Write-Host "`nSetup completed!" -ForegroundColor Green
Write-Host "`nNext steps:" -ForegroundColor Cyan
Write-Host "1. Configure your AWS credentials: aws configure" -ForegroundColor White
Write-Host "2. Create your S3 bucket in AWS Console" -ForegroundColor White
Write-Host "3. Test AWS services: .\test-aws-services.ps1" -ForegroundColor White
Write-Host "4. Start the video translation service: docker-compose up -d" -ForegroundColor White
Write-Host ""
Write-Host "For detailed AWS setup instructions, see the README.md file" -ForegroundColor Cyan 