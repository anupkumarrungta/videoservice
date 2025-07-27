# SSML Synthesis Fix

## Problem Identified

The error `InvalidSsmlException: Invalid SSML request` was caused by malformed SSML generation in the audio synthesis process.

## Root Causes

1. **Invalid SSML Tags**: Complex regex replacements were creating malformed SSML
2. **Unescaped Characters**: Special characters in text weren't properly escaped for SSML
3. **Voice ID Issues**: Invalid voice IDs were being used in SSML
4. **Complex Emphasis**: Overly complex emphasis tags were causing SSML validation failures

## Fixes Implemented

### 1. Robust SSML Generation

#### Enhanced Error Handling
```java
// Added fallback mechanism
try {
    // Enhanced SSML synthesis
    return synthesizeSpeechWithSSML(ssmlText, language, outputFile);
} catch (Exception ssmlError) {
    // Fallback to basic synthesis
    return synthesizeSpeechWithGender(text, language, outputFile, detectedGender);
}
```

#### Text Escaping for SSML
```java
private String escapeTextForSSML(String text) {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
        .replaceAll("[\\x00-\\x1F\\x7F]", ""); // Remove control characters
}
```

#### Simplified SSML Pauses
```java
private String addSimpleSSMLPauses(String text) {
    // Simple, safe pauses instead of complex emphasis
    String result = text.replaceAll("([.!?।])\\s+", "$1<break time=\"0.7s\"/> ");
    result = result.replaceAll("([,;])\\s+", "$1<break time=\"0.3s\"/> ");
    return result;
}
```

### 2. Validation and Logging

#### SSML Validation
- Check for null/empty SSML text
- Validate voice ID before use
- Log SSML preview for debugging
- Detailed error messages for SSML failures

#### Enhanced Logging
```java
// Log SSML preview for debugging
String ssmlPreview = ssmlText.length() > 200 ? ssmlText.substring(0, 200) + "..." : ssmlText;
logger.info("SSML preview: {}", ssmlPreview);
```

### 3. Fallback Strategy

The system now uses a progressive approach:

1. **Primary**: Enhanced SSML with natural pauses and modulation
2. **Fallback**: Basic synthesis with gender detection
3. **Error Handling**: Graceful degradation with detailed logging

## Expected Results

### Before Fix
- ❌ `InvalidSsmlException` causing job failures
- ❌ No fallback mechanism
- ❌ Poor error reporting

### After Fix
- ✅ Robust SSML generation with proper escaping
- ✅ Automatic fallback to basic synthesis
- ✅ Detailed logging for debugging
- ✅ Graceful error handling

## Testing Approach

### 1. Test SSML Generation
```java
// Test with various text inputs
String testText = "Hello, world! This is a test.";
String ssml = createEnhancedSSML(testText, "en", "default", 1.0);
// Should generate valid SSML without exceptions
```

### 2. Test Fallback Mechanism
```java
// Test with problematic text that might cause SSML issues
String problematicText = "Text with & < > \" ' characters";
// Should fallback to basic synthesis if SSML fails
```

### 3. Test Error Handling
```java
// Test with invalid voice IDs or language codes
// Should provide clear error messages and fallback
```

## Monitoring

### Key Metrics to Watch
- SSML synthesis success rate
- Fallback method usage frequency
- SSML validation error patterns
- Audio synthesis completion rates

### Log Analysis
- Look for "Enhanced SSML synthesis failed" warnings
- Monitor "Falling back to basic synthesis" messages
- Check SSML preview logs for malformed content
- Verify audio file creation success rates

## Configuration

### SSML Settings
```yaml
tts:
  aws:
    engine: standard  # Use standard engine for better SSML support
    max-ssml-length: 3000  # Limit SSML length to avoid issues
```

### Audio Settings
```yaml
audio:
  sample-rate: 16000
  bit-rate: 128000
  enable-ssml-fallback: true  # Enable fallback mechanism
```

This fix ensures that the audio synthesis process is robust and can handle various edge cases while maintaining the enhanced speech quality when possible. 