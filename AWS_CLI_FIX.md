# AWS CLI Fix Guide

## Current Issue
Your AWS CLI is experiencing parsing errors and 400 errors when calling AWS services. This is likely due to an outdated AWS CLI version (2.27.49) and potential configuration issues.

## Solution Steps

### Step 1: Update AWS CLI
Your current version is `aws-cli/2.27.49` which is quite old. The current version is much newer.

**Download and install the latest AWS CLI:**
1. Go to: https://aws.amazon.com/cli/
2. Download the latest Windows installer
3. Run the installer and follow the prompts
4. Restart your terminal/PowerShell

**Or use the MSI installer:**
```powershell
# Download the latest MSI installer
Invoke-WebRequest -Uri "https://awscli.amazonaws.com/AWSCLIV2.msi" -OutFile "AWSCLIV2.msi"

# Install it
Start-Process msiexec.exe -Wait -ArgumentList '/I AWSCLIV2.msi /quiet'
```

### Step 2: Verify Installation
After updating, verify the new version:
```powershell
aws --version
```

You should see a much newer version (like 2.x.x or 3.x.x).

### Step 3: Reconfigure AWS
After updating, reconfigure your AWS credentials:
```powershell
aws configure
```

Enter your:
- AWS Access Key ID
- AWS Secret Access Key  
- Default region: `us-east-1`
- Default output format: `json`

### Step 4: Test Basic Commands
Test that basic AWS commands work:
```powershell
# Test credentials
aws sts get-caller-identity

# Test S3
aws s3 ls s3://video-translation-bucket2

# Test Transcribe
aws transcribe list-transcription-jobs
```

### Step 5: Alternative - Use AWS SDK Directly
If CLI issues persist, the application can work directly with AWS SDK. The Java application uses the AWS SDK, not the CLI, so it should work even if CLI has issues.

## Alternative Solution: Skip CLI Testing

Since your application uses the AWS SDK directly (not the CLI), you can skip the CLI testing and test the application directly:

### Step 1: Update the Debug Script
Modify `debug-aws-issues.ps1` to skip the problematic CLI tests:

```powershell
# Comment out or remove the transcribe CLI test
# $transcribeTest = aws transcribe list-transcription-jobs 2>&1
```

### Step 2: Test the Application Directly
1. **Restart your application**
2. **Upload a short test video** (30 seconds) with English audio
3. **Monitor the application logs** for:
   - AWS SDK connection messages
   - Transcription job creation
   - Real transcription results (not mock data)

### Step 3: Check Application Logs
Look for these log messages:
- `[Transcription] Starting AWS Transcribe for file:`
- `[Transcription] Transcription job started successfully:`
- `[Transcription] SUCCESS: Transcription completed!`

If you see these, AWS Transcribe is working through the SDK.

## Quick Test Commands

### Test 1: Basic AWS SDK (Application Level)
```powershell
# Start your application and check logs
# Look for AWS SDK connection messages
```

### Test 2: Manual AWS Console Test
1. Go to AWS Console → Amazon Transcribe
2. Try to create a transcription job manually
3. If it works in console, the service is available

### Test 3: Check Network/Proxy
```powershell
# Test network connectivity
Test-NetConnection -ComputerName "transcribe.us-east-1.amazonaws.com" -Port 443
```

## Expected Results After Fix

### If AWS CLI is updated:
- ✅ `aws sts get-caller-identity` returns valid JSON
- ✅ `aws transcribe list-transcription-jobs` works
- ✅ No more parsing errors

### If using application directly:
- ✅ Application logs show real transcription jobs
- ✅ No more "mock transcription" warnings
- ✅ Real Hindi translation of English audio
- ✅ Full video duration preserved

## Troubleshooting

### If CLI still fails after update:
1. Check for corporate proxy/firewall
2. Try different AWS regions
3. Use AWS SDK directly (application should work)

### If application still uses mock data:
1. Check application logs for specific AWS errors
2. Verify IAM permissions are correct
3. Ensure S3 bucket exists and is accessible

## Next Steps

1. **Update AWS CLI** to the latest version
2. **Reconfigure AWS credentials**
3. **Test basic commands**
4. **Restart your application**
5. **Test with a short video**

The application should work even if CLI has issues, since it uses the AWS SDK directly. 