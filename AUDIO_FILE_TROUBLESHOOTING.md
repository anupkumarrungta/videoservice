# Audio File Troubleshooting Guide

## Overview
This guide helps resolve "Invalid media file" errors that occur during AWS Transcribe processing.

## Common Error Messages

### Primary Error
```
The data in your input media file isn't valid. Check the media file and try your request again.
```

### Related Errors
- `Invalid media file detected`
- `No audio stream found in file`
- `Audio duration too short/long`
- `File is corrupted or has no audio content`

## Root Causes and Solutions

### 1. **No Audio Content**
**Symptoms**: File exists but contains no audio stream
**Solutions**:
- Ensure the video file actually contains audio
- Check if audio was properly extracted from video
- Verify the original source has audio content

### 2. **Unsupported Audio Format**
**Symptoms**: Audio exists but format is not compatible
**Supported Formats**:
- ✅ MP3 (recommended)
- ✅ WAV
- ✅ FLAC
- ✅ M4A
- ✅ MP4 (with audio)
- ✅ AVI (with audio)
- ✅ MOV (with audio)
- ✅ MKV (with audio)

**Solutions**:
- Convert to MP3 format using FFmpeg
- Ensure proper audio codec (AAC, MP3, PCM)

### 3. **Audio Duration Issues**
**Symptoms**: File too short or too long
**Limits**:
- **Minimum**: 0.5 seconds
- **Maximum**: 1 hour (3600 seconds)

**Solutions**:
- For short files: Add silence or combine with other audio
- For long files: Split into smaller chunks

### 4. **Audio Quality Issues**
**Symptoms**: Poor audio quality causing transcription failure
**Requirements**:
- **Sample Rate**: 8kHz - 48kHz (16kHz recommended)
- **Channels**: Mono or Stereo
- **Bitrate**: 64kbps - 320kbps (128kbps recommended)

**Solutions**:
- Convert to 16kHz mono MP3
- Improve audio quality before processing
- Remove background noise

### 5. **File Corruption**
**Symptoms**: File exists but is corrupted
**Solutions**:
- Re-extract audio from original source
- Use a different audio extraction method
- Check file integrity with media player

## Automatic Fixes Implemented

### 1. **Audio Validation**
The system now validates audio files before transcription:
```java
validateAudioFile(audioFile);
```

**Checks**:
- File existence and readability
- File size (1KB - 100MB)
- Supported file formats
- Audio content presence

### 2. **Audio Conversion**
Automatic conversion to AWS Transcribe compatible format:
```java
File convertedAudioFile = convertAudioForTranscription(audioFile);
```

**Conversion Settings**:
- **Format**: MP3
- **Sample Rate**: 16kHz
- **Channels**: Mono
- **Bitrate**: 128kbps

### 3. **Error Analysis**
Detailed analysis of problematic files:
```java
analyzeProblematicFile(s3Key);
```

**Analysis Includes**:
- Audio stream detection
- Codec information
- Duration and size validation
- Quality metrics

## Manual Troubleshooting Steps

### Step 1: Check File Properties
```bash
# Using FFmpeg
ffprobe -v quiet -show_entries format=duration,size -show_streams -of json your_file.mp3
```

### Step 2: Validate Audio Content
```bash
# Check if file has audio
ffprobe -v quiet -select_streams a:0 -show_entries stream=codec_type -of csv=p=0 your_file.mp3
```

### Step 3: Convert to Compatible Format
```bash
# Convert to AWS Transcribe compatible format
ffmpeg -i input_file.mp4 -acodec mp3 -ar 16000 -ac 1 -b:a 128k output_file.mp3
```

### Step 4: Test with Media Player
- Open file in VLC, Windows Media Player, or similar
- Verify audio plays correctly
- Check audio properties

## Prevention Best Practices

### 1. **Audio Extraction**
```bash
# Extract audio from video with good quality
ffmpeg -i video.mp4 -vn -acodec mp3 -ar 16000 -ac 1 -b:a 128k audio.mp3
```

### 2. **Quality Checks**
- Always verify audio content before processing
- Use consistent audio settings
- Test with small samples first

### 3. **File Management**
- Keep original files as backup
- Use descriptive file names
- Organize files by language/content type

## Debugging Information

### System Logs
The system provides detailed logging for troubleshooting:

```
[Audio Validation] Starting validation for: audio_chunk_001.mp3
[Audio Validation] File size: 245760 bytes
[Audio Validation] Audio duration: 15.2 seconds
[Audio Validation] Audio content validation passed
[Audio Conversion] Converting audio for AWS Transcribe compatibility
[Audio Conversion] Audio conversion completed successfully
```

### Error Analysis
When errors occur, the system provides:

```
[File Analysis] Analyzing problematic file: transcription/uuid/audio.mp3
[File Analysis] Audio stream found
[File Analysis] Audio codec: mp3
[File Analysis] Sample rate: 16000 Hz
[File Analysis] Channels: 1
[File Analysis] Duration: 15.2 seconds
```

## Common Scenarios

### Scenario 1: Video with No Audio
**Problem**: Video file has no audio track
**Solution**: Extract audio from a different source or add audio

### Scenario 2: Corrupted Audio File
**Problem**: Audio file is corrupted during processing
**Solution**: Re-extract from original source with different method

### Scenario 3: Unsupported Format
**Problem**: Audio format not supported by AWS Transcribe
**Solution**: Convert to MP3 using the automatic conversion

### Scenario 4: Audio Too Short
**Problem**: Audio duration less than 0.5 seconds
**Solution**: Combine with other audio or add silence

## Support Information

### When to Contact Support
- All automatic fixes have been tried
- File analysis shows valid audio but still fails
- Consistent failures with multiple files
- Unusual error messages

### Information to Provide
- Original error message
- File format and size
- Audio duration
- System logs
- File analysis output

## Conclusion

The enhanced audio processing system now:
1. **Validates** audio files before processing
2. **Converts** files to compatible formats automatically
3. **Analyzes** problematic files for detailed diagnostics
4. **Provides** comprehensive error messages and solutions

Most audio file issues can be resolved automatically by the system, but this guide helps with manual troubleshooting when needed. 