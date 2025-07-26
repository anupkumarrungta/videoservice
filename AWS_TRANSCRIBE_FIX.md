# AWS Transcribe Fix Guide

## Current Status
Based on the debug output, your AWS setup is mostly working:
- ✅ AWS CLI is installed and working
- ✅ AWS credentials are valid
- ✅ S3 bucket access is working
- ❌ **AWS Transcribe service access failed**
- ✅ AWS Translate service is working
- ✅ AWS Polly service is working

## The Problem
The AWS Transcribe service is not accessible, which means your video translation will fail and fall back to mock data.

## Solution Steps

### Step 1: Check IAM Permissions
Your IAM user `video-translation-service` needs the following permissions:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "TranscribePermissions",
            "Effect": "Allow",
            "Action": [
                "transcribe:StartTranscriptionJob",
                "transcribe:GetTranscriptionJob",
                "transcribe:ListTranscriptionJobs",
                "transcribe:DeleteTranscriptionJob"
            ],
            "Resource": "*"
        },
        {
            "Sid": "S3TranscribeBucketPermissions",
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:GetObject",
                "s3:DeleteObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::video-translation-bucket2",
                "arn:aws:s3:::video-translation-bucket2/*"
            ]
        }
    ]
}
```

### Step 2: Add IAM Permissions
1. Go to AWS Console → IAM → Users → video-translation-service
2. Click "Add permissions" → "Attach policies directly"
3. Create a new policy with the JSON above, or attach these managed policies:
   - `AmazonTranscribeFullAccess`
   - `AmazonS3FullAccess` (or create a custom policy for your specific bucket)

### Step 3: Verify AWS Region
Make sure your AWS region supports Transcribe. Run:
```powershell
aws configure get region
```

If it's not set to a supported region, set it:
```powershell
aws configure set region us-east-1
```

### Step 4: Test Transcribe Service
Run the updated debug script:
```powershell
.\debug-aws-issues.ps1
```

### Step 5: Manual Test
Test AWS Transcribe manually:
```powershell
# Test basic transcribe access
aws transcribe help

# Test listing transcription jobs
aws transcribe list-transcription-jobs

# Test creating a simple transcription job (optional)
aws transcribe start-transcription-job --transcription-job-name test-job --media MediaFileUri=s3://video-translation-bucket2/test-audio.mp3 --language-code en-US
```

## Alternative Solutions

### Option 1: Use AWS Console
1. Go to AWS Console → Amazon Transcribe
2. Try to create a transcription job manually
3. If it works in console but not CLI, it's a CLI configuration issue

### Option 2: Check AWS CLI Version
Update AWS CLI to the latest version:
```powershell
# Check current version
aws --version

# Update if needed (download from AWS website)
# https://aws.amazon.com/cli/
```

### Option 3: Use Different Region
If your current region doesn't support Transcribe, switch to a supported region:
```powershell
aws configure set region us-east-1
```

## Expected Result
After fixing the permissions, the debug script should show:
```
4. Testing AWS Transcribe service...
✓ AWS Transcribe CLI is available
✓ AWS Transcribe service is accessible
```

## Testing the Application
Once AWS Transcribe is working:

1. **Restart your application**
2. **Upload a test video** with English audio
3. **Monitor the logs** for:
   - No more "mock transcription" warnings
   - Successful AWS Transcribe job creation
   - Real transcription results

## Troubleshooting

### If IAM permissions are correct but still failing:
1. Check if Transcribe is enabled in your AWS account
2. Verify you're not in a restricted region
3. Check for any AWS service quotas or limits

### If the application still uses mock data:
1. Check the application logs for specific AWS errors
2. Verify the source language is set to "english" in your request
3. Ensure the S3 bucket exists and is accessible

## Quick Fix Commands

```powershell
# Set region
aws configure set region us-east-1

# Test transcribe
aws transcribe list-transcription-jobs

# If successful, restart your application and test
```

## Next Steps
1. Fix the IAM permissions
2. Run the debug script again
3. Test the application with a short video first
4. Monitor logs for real transcription results 