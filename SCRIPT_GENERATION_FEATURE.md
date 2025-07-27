# Script Generation Feature

## Overview

The Video Translation Service now generates **script documents** alongside translated videos, providing users with easy-to-read text versions of both source and target language content. These documents are stored in the S3 bucket and can be viewed inline for reference and verification.

## Problem Solved

### Before Feature:
- ❌ No text version of translated content available
- ❌ Difficult to verify translation accuracy
- ❌ No easy way to read the script without playing the video
- ❌ No side-by-side comparison of source and target languages

### After Feature:
- ✅ Text documents generated automatically
- ✅ Side-by-side comparison of source and target scripts
- ✅ Easy verification of translation quality
- ✅ Readable format for quick reference
- ✅ Stored in S3 bucket alongside videos

## Features Implemented

### 1. ScriptGenerationService
A dedicated service for creating script documents with the following capabilities:

#### Methods:
- `generateScriptDocument()` - Creates side-by-side comparison document
- `generateSimpleScript()` - Creates simple sequential format document
- `writeDocumentHeader()` - Adds metadata and formatting
- `writeScriptsSideBySide()` - Formats scripts in parallel columns
- `cleanAndFormatScript()` - Improves readability of script text

### 2. Two Document Formats

#### A. Side-by-Side Format
```
================================================================================
VIDEO TRANSLATION SCRIPT
================================================================================
Generated: 2024-01-15 14:30:25
Source Language: English
Target Language: Hindi
================================================================================

SOURCE (ENGLISH)                    | TARGET (HINDI)
----------------------------------------+----------------------------------------

CHUNK 1:
----------------------------------------+----------------------------------------
Hello, my name is Ayan.               | नमस्ते, मेरा नाम Ayan है।
I am from Mumbai.                     | मैं Mumbai से हूं।

================================================================================

CHUNK 2:
----------------------------------------+----------------------------------------
I work as a software engineer.        | मैं एक software engineer के रूप में काम करता हूं।
```

#### B. Simple Format
```
VIDEO TRANSLATION SCRIPT
Source: English | Target: Hindi
Generated: 2024-01-15 14:30:25
============================================================

CHUNK 1:
------------------------------
SOURCE (English):
Hello, my name is Ayan. I am from Mumbai.

TARGET (Hindi):
नमस्ते, मेरा नाम Ayan है। मैं Mumbai से हूं।

==============================

CHUNK 2:
------------------------------
SOURCE (English):
I work as a software engineer.

TARGET (Hindi):
मैं एक software engineer के रूप में काम करता हूं।

==============================
```

## Technical Implementation

### 1. Integration with JobManager
```java
// Added to JobManager constructor
private final ScriptGenerationService scriptGenerationService;

// Script collection during processing
List<String> sourceScripts = new ArrayList<>();
List<String> targetScripts = new ArrayList<>();

// Collect scripts for each chunk
sourceScripts.add(transcribedText);
targetScripts.add(translatedText);
```

### 2. Document Generation Process
```java
// Generate both document formats
File sideBySideScript = scriptGenerationService.generateScriptDocument(
    sourceScripts, targetScripts, sourceLanguage, targetLanguage, tempDir);

File simpleScript = scriptGenerationService.generateSimpleScript(
    sourceScripts, targetScripts, sourceLanguage, targetLanguage, tempDir);
```

### 3. S3 Storage
```java
// Upload to S3 with descriptive keys
String sideBySideScriptKey = job.getId() + "_" + targetLanguage + "_script_side_by_side.txt";
String simpleScriptKey = job.getId() + "_" + targetLanguage + "_script_simple.txt";

s3StorageService.uploadFileWithKey(sideBySideScript, sideBySideScriptKey);
s3StorageService.uploadFileWithKey(simpleScript, simpleScriptKey);
```

## File Naming Convention

### Generated Files:
- **Side-by-side script**: `{jobId}_{targetLanguage}_script_side_by_side.txt`
- **Simple script**: `{jobId}_{targetLanguage}_script_simple.txt`
- **Timestamp**: Included in filename for uniqueness

### Examples:
- `6db247e6-a308-42f4-8fc1-de455fd71570_hindi_script_side_by_side.txt`
- `6db247e6-a308-42f4-8fc1-de455fd71570_hindi_script_simple.txt`

## Document Features

### 1. Metadata Header
- Generation timestamp
- Source and target languages
- Job information
- Clear formatting with separators

### 2. Chunk Organization
- Each audio chunk gets its own section
- Clear chunk numbering (CHUNK 1, CHUNK 2, etc.)
- Proper separation between chunks

### 3. Text Formatting
- Cleaned and normalized text
- Proper sentence endings
- Truncated long lines for readability
- Word boundary preservation

### 4. Error Handling
- Graceful handling of missing scripts
- Fallback to local storage if S3 fails
- Non-blocking script generation (doesn't fail the job)

## Usage Examples

### 1. Viewing Scripts in S3
```bash
# Download side-by-side script
aws s3 cp s3://video-translation-bucket2/6db247e6-a308-42f4-8fc1-de455fd71570_hindi_script_side_by_side.txt ./script.txt

# View the script
cat script.txt
```

### 2. Programmatic Access
```java
// Get script URLs from S3
String sideBySideUrl = s3StorageService.getFileUrl(sideBySideScriptKey);
String simpleUrl = s3StorageService.getFileUrl(simpleScriptKey);

// Download and process scripts
// ... implementation details
```

### 3. Web Interface Integration
```html
<!-- Add script download links to UI -->
<a href="${sideBySideScriptUrl}" target="_blank">Download Side-by-Side Script</a>
<a href="${simpleScriptUrl}" target="_blank">Download Simple Script</a>
```

## Benefits

### 1. Quality Assurance
- **Easy Verification**: Quickly check translation accuracy
- **Content Review**: Review scripts without playing video
- **Error Detection**: Identify translation issues early

### 2. Accessibility
- **Text Format**: Accessible to screen readers
- **Searchable**: Find specific content easily
- **Printable**: Can be printed for offline review

### 3. Collaboration
- **Share Scripts**: Share text versions with stakeholders
- **Review Process**: Enable script review workflows
- **Documentation**: Create permanent records of translations

### 4. Analysis
- **Content Analysis**: Analyze translation patterns
- **Quality Metrics**: Measure translation quality
- **Training Data**: Use for improving translation models

## Monitoring and Logging

### Key Log Messages:
```
[Script Generation] Generating script document for English to Hindi translation
[Script Generation] Source scripts: 5 chunks
[Script Generation] Target scripts: 5 chunks
[Translation Pipeline] Collected scripts for chunk 0 - Source: 45 chars, Target: 52 chars
[Translation Pipeline] SUCCESS: Script documents uploaded to S3
[Translation Pipeline] Script document generation completed successfully
```

### Success Indicators:
- Script documents generated for each language pair
- Files uploaded to S3 successfully
- Proper file naming and organization
- Readable document formatting

### Error Handling:
- Script generation failures don't block video processing
- Fallback to local storage if S3 upload fails
- Comprehensive error logging for debugging

## Future Enhancements

### 1. Multiple Formats
- **PDF Generation**: Create PDF versions of scripts
- **Word Documents**: Generate .docx files
- **HTML Format**: Web-friendly script viewing

### 2. Advanced Features
- **Timestamps**: Add timing information for each chunk
- **Speaker Labels**: Include speaker identification
- **Confidence Scores**: Show transcription confidence

### 3. Integration
- **Email Attachments**: Send scripts via email
- **API Endpoints**: REST API for script access
- **Web Viewer**: In-browser script viewing

### 4. Analytics
- **Script Analytics**: Analyze translation patterns
- **Quality Metrics**: Measure script quality
- **Usage Tracking**: Track script downloads and views

## Configuration

### File Settings:
```yaml
# Script generation settings
script:
  max-line-length: 40
  include-timestamps: false
  include-speaker-labels: false
  generate-pdf: false
  generate-word: false
```

### S3 Settings:
```yaml
# S3 storage settings
aws:
  s3:
    bucket-name: video-translation-bucket2
    script-prefix: scripts/
    retention-days: 30
```

This feature significantly enhances the user experience by providing easy access to translated content in a readable, searchable format, making it simple to verify translation quality and review content without playing videos. 