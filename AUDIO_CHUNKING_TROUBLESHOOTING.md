# Audio Chunking Troubleshooting Guide

## Overview
This guide helps resolve issues with audio chunking that can cause "Invalid media file" errors during transcription.

## Problem Identified
```
Audio file details: name=chunk_002.mp3, size=433, languageCode=hi-IN
```
The audio chunk is only **433 bytes**, which is extremely small and indicates the chunking process is creating invalid files.

## Root Causes

### 1. **Chunk Duration Too Small**
**Problem**: Chunks are being created with very short durations (< 0.5 seconds)
**Solution**: Minimum chunk duration enforced (5 seconds)

### 2. **FFmpeg Library Issues**
**Problem**: Java FFmpeg library may not handle chunking correctly
**Solution**: Direct FFmpeg command execution

### 3. **Audio Format Compatibility**
**Problem**: Output format not compatible with AWS Transcribe
**Solution**: Standardized MP3 format with optimal settings

### 4. **File Size Validation**
**Problem**: Chunks created but too small for transcription
**Solution**: Minimum file size validation (1KB)

## Fixes Implemented

### 1. **Enhanced Audio Chunking**
```java
// Direct FFmpeg command for reliable chunking
ProcessBuilder processBuilder = new ProcessBuilder(
    "C:\\ffmpeg\\bin\\ffmpeg.exe",
    "-i", audioFile.getAbsolutePath(),
    "-ss", String.format("%.3f", startTime),
    "-t", String.format("%.3f", chunkDuration),
    "-acodec", "libmp3lame",
    "-ar", "16000",  // 16kHz sample rate for transcription
    "-ac", "1",      // Mono channel
    "-b:a", "128k",  // 128kbps bitrate
    "-y",            // Overwrite output file
    chunkFile.getAbsolutePath()
);
```

### 2. **Minimum Duration Enforcement**
```java
// Ensure minimum chunk duration
if (chunkDurationSeconds < 5) {
    logger.warn("[Audio Chunking] Chunk duration too small ({}s), setting to minimum 5 seconds", chunkDurationSeconds);
    chunkDurationSeconds = 5;
}

// Skip chunks that are too short
if (chunkDuration < 0.5) {
    logger.warn("[Audio Chunking] Skipping chunk {} - duration too short: {}s", i, chunkDuration);
    continue;
}
```

### 3. **File Size Validation**
```java
// Verify the chunk was created and has content
if (chunkFile.exists() && chunkFile.length() > 1024) { // At least 1KB
    chunkFiles.add(chunkFile);
    logger.info("[Audio Chunking] Created audio chunk {}: {} ({} bytes)", i, chunkFile.getName(), chunkFile.length());
} else {
    logger.error("[Audio Chunking] Chunk {} was created but is too small ({} bytes)", i, chunkFile.exists() ? chunkFile.length() : 0);
    throw new IOException("Audio chunk " + i + " was not created properly (too small)");
}
```

### 4. **Audio Content Validation**
```java
// Additional validation: check if chunk has audio content
try {
    double chunkDurationActual = getAudioDuration(chunkFile);
    logger.info("[Audio Chunking] Chunk {} actual duration: {}s", i, chunkDurationActual);
    
    if (chunkDurationActual < 0.5) {
        logger.warn("[Audio Chunking] Chunk {} duration too short ({}s), removing", i, chunkDurationActual);
        chunkFile.delete();
        continue;
    }
} catch (Exception e) {
    logger.warn("[Audio Chunking] Could not validate chunk {} duration: {}", i, e.getMessage());
}
```

### 5. **Pre-Transcription Validation**
```java
// Validate audio chunks before transcription
audioChunks = videoProcessingService.validateAudioChunks(audioChunks);
```

## Audio Chunking Process

### Step 1: Audio Duration Analysis
- Get total audio duration using FFprobe
- Calculate optimal number of chunks
- Ensure minimum chunk duration (5 seconds)

### Step 2: Chunk Creation
- Use direct FFmpeg commands for reliability
- Apply optimal audio settings for transcription
- Validate each chunk immediately after creation

### Step 3: Quality Assurance
- Check file size (minimum 1KB)
- Verify audio duration (minimum 0.5 seconds)
- Validate audio content and codec
- Remove invalid chunks

### Step 4: Pre-Transcription Validation
- Comprehensive validation of all chunks
- Filter out problematic chunks
- Ensure at least one valid chunk exists

## Optimal Audio Settings

### For AWS Transcribe Compatibility
| Parameter | Value | Reason |
|-----------|-------|--------|
| **Format** | MP3 | Best compatibility |
| **Sample Rate** | 16kHz | Optimal for transcription |
| **Channels** | Mono | Reduces complexity |
| **Bitrate** | 128kbps | Good quality, reasonable size |
| **Codec** | libmp3lame | Reliable MP3 encoding |

### Chunk Duration Guidelines
| Duration | Status | Use Case |
|----------|--------|----------|
| **< 0.5s** | ❌ Invalid | Too short for transcription |
| **0.5s - 5s** | ⚠️ Risky | May cause issues |
| **5s - 60s** | ✅ Optimal | Best for transcription |
| **60s - 300s** | ✅ Good | Acceptable for longer content |
| **> 300s** | ⚠️ Large | May timeout or fail |

## Error Prevention

### 1. **Input Validation**
- Check source audio file integrity
- Verify audio content exists
- Ensure reasonable file size

### 2. **Process Monitoring**
- Log each chunk creation step
- Capture FFmpeg error output
- Validate chunks immediately

### 3. **Quality Checks**
- File size validation
- Duration verification
- Audio content analysis
- Codec compatibility check

### 4. **Error Recovery**
- Remove invalid chunks
- Retry with different settings
- Fallback to larger chunks
- Comprehensive error reporting

## Debugging Information

### System Logs
```
[Audio Chunking] Splitting audio into 30s chunks: audio.mp3
[Audio Chunking] Audio file size: 2457600 bytes
[Audio Chunking] Total audio duration: 120.5 seconds
[Audio Chunking] Will create 4 chunks
[Audio Chunking] Creating chunk 0: start=0.0s, end=30.0s, duration=30.0s
[Audio Chunking] FFmpeg command: ffmpeg -i audio.mp3 -ss 0.000 -t 30.000 -acodec libmp3lame -ar 16000 -ac 1 -b:a 128k -y chunk_000.mp3
[Audio Chunking] Created audio chunk 0: chunk_000.mp3 (245760 bytes)
[Audio Chunking] Chunk 0 actual duration: 30.0s
```

### Error Analysis
```
[Audio Validation] Validating 4 audio chunks for transcription
[Audio Validation] Chunk 0: 245760 bytes, 30.0s duration
[Audio Validation] Chunk 0 is valid
[Audio Validation] Validation complete: 4 valid chunks out of 4
```

## Common Issues and Solutions

### Issue 1: Chunks Too Small
**Symptoms**: Chunk files < 1KB
**Solution**: Increase minimum chunk duration, validate source audio

### Issue 2: No Audio Content
**Symptoms**: Chunks exist but no audio stream
**Solution**: Check source file, verify audio extraction

### Issue 3: Invalid Format
**Symptoms**: Unsupported codec errors
**Solution**: Convert to MP3 with standard settings

### Issue 4: Duration Mismatch
**Symptoms**: Expected vs actual duration different
**Solution**: Use direct FFmpeg commands, validate timing

## Best Practices

### 1. **Chunk Duration**
- Use 30-60 seconds for optimal results
- Avoid chunks shorter than 5 seconds
- Consider content boundaries (sentences, paragraphs)

### 2. **Quality Settings**
- Use consistent audio parameters
- Optimize for transcription accuracy
- Balance quality vs file size

### 3. **Error Handling**
- Validate at each step
- Provide detailed error messages
- Implement fallback strategies

### 4. **Monitoring**
- Log all chunking operations
- Track success/failure rates
- Monitor file sizes and durations

## Conclusion

The enhanced audio chunking system now:
1. **Validates** input audio files thoroughly
2. **Creates** chunks with optimal settings for transcription
3. **Verifies** each chunk immediately after creation
4. **Filters** out invalid chunks before transcription
5. **Provides** comprehensive logging for debugging

This should resolve the "Invalid media file" errors caused by problematic audio chunks. 