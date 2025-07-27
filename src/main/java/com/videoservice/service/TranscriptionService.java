package com.videoservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.transcribe.TranscribeClient;
import software.amazon.awssdk.services.transcribe.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Service for handling speech-to-text transcription using AWS Transcribe.
 * Supports real-time transcription with fallback to mock transcription.
 */
@Service
public class TranscriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TranscriptionService.class);
    
    private final TranscribeClient transcribeClient;
    private final S3Client s3Client;
    
    @Value("${aws.s3.bucket-name:video-translation-bucket2}")
    private String bucketName;
    
    @Value("${aws.region:us-east-1}")
    private String awsRegion;
    
    public TranscriptionService(TranscribeClient transcribeClient, S3Client s3Client) {
        this.transcribeClient = transcribeClient;
        this.s3Client = s3Client;
        logger.info("TranscriptionService initialized with AWS Transcribe support");
        logger.info("TranscriptionService configured with region: {}", awsRegion);
        
        // Verify S3 bucket access on startup
        verifyS3BucketAccess();
    }
    
    /**
     * Transcribe audio file to text using AWS Transcribe.
     * 
     * @param audioFile The audio file to transcribe
     * @param languageCode The language code (e.g., "en-US", "hi-IN")
     * @return The transcribed text
     * @throws Exception if transcription fails
     */
    public String transcribeAudio(File audioFile, String languageCode) throws Exception {
        // First, let's verify the audio file is valid
        logger.info("[Transcription] Verifying audio file before transcription");
        logger.info("[Transcription] Audio file: {} ({} bytes)", audioFile.getAbsolutePath(), audioFile.length());
        
        if (!audioFile.exists()) {
            throw new IOException("Audio file does not exist: " + audioFile.getAbsolutePath());
        }
        
        if (audioFile.length() == 0) {
            throw new IOException("Audio file is empty: " + audioFile.getAbsolutePath());
        }
        
        // Check if the audio file is too small (less than 1KB might be invalid)
        if (audioFile.length() < 1024) {
            logger.warn("[Transcription] Audio file is very small ({} bytes), this might indicate an issue", audioFile.length());
        }
        logger.info("[Transcription] Starting AWS Transcribe for file: {} with language: {}", audioFile.getName(), languageCode);
        logger.info("[Transcription] Audio file size: {} bytes", audioFile.length());
        
        try {
            // Validate the audio file
            validateAudioFile(audioFile);
            
            // Convert audio to a compatible format for AWS Transcribe
            File convertedAudioFile = convertAudioForTranscription(audioFile);
            
            // Upload audio file to S3 for transcription
            String s3Key = "transcription/" + UUID.randomUUID() + "/" + audioFile.getName();
            logger.info("[Transcription] Uploading audio to S3: {}", s3Key);
            uploadAudioToS3(convertedAudioFile, s3Key);
            
            // Call the S3-based transcription method
            return transcribeAudioFromS3(s3Key, languageCode);
            
        } catch (Exception e) {
            logger.error("AWS Transcribe failed with error: {}", e.getMessage());
            logger.error("Audio file details: name={}, size={}, languageCode={}", audioFile.getName(), audioFile.length(), languageCode);
            logger.error("Stack trace:", e);
            
            // Don't use mock transcription - throw the error instead
            logger.error("AWS Transcribe failed. Please check your AWS credentials and permissions.");
            logger.error("Required permissions: transcribe:StartTranscriptionJob, transcribe:GetTranscriptionJob, s3:PutObject, s3:GetObject");
            throw new Exception("AWS Transcribe failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get mock transcription text based on language.
     */
    private String getMockTranscription(String languageCode, String fileName) {
        logger.info("[Mock Transcription] Generating mock transcription for language: {}", languageCode);
        
        if (languageCode.startsWith("hi")) {
            String hindiText = "नमस्ते दोस्तों, आज मैं आपको एक महत्वपूर्ण बात बताना चाहता हूं। यह वीडियो आपके लिए बहुत उपयोगी होगी। मैं आपको स्टेप बाय स्टेप समझाऊंगा कि कैसे इस प्रोसेस को फॉलो करना है। ध्यान से सुनिए और नोट्स बनाइए।";
            logger.info("[Mock Transcription] Generated Hindi text: {}", hindiText);
            return hindiText;
        } else if (languageCode.startsWith("en")) {
            String englishText = "Hello friends, today I want to tell you something important. This video will be very useful for you. I will explain to you step by step how to follow this process. Listen carefully and take notes.";
            logger.info("[Mock Transcription] Generated English text: {}", englishText);
            return englishText;
        } else if (languageCode.startsWith("es")) {
            return "Hola amigos, hoy quiero contarte algo importante. Este video será muy útil para ti. Te explicaré paso a paso cómo seguir este proceso. Escucha con atención y toma notas.";
        } else if (languageCode.startsWith("fr")) {
            return "Bonjour les amis, aujourd'hui je veux vous dire quelque chose d'important. Cette vidéo sera très utile pour vous. Je vous expliquerai étape par étape comment suivre ce processus. Écoutez attentivement et prenez des notes.";
        } else if (languageCode.startsWith("de")) {
            return "Hallo Freunde, heute möchte ich Ihnen etwas Wichtiges erzählen. Dieses Video wird sehr nützlich für Sie sein. Ich werde Ihnen Schritt für Schritt erklären, wie Sie diesem Prozess folgen können. Hören Sie genau zu und machen Sie sich Notizen.";
        } else {
            String defaultText = "This is a mock transcription for the video file: " + fileName + ". In a real implementation, this would be the actual transcribed text from the audio.";
            logger.info("[Mock Transcription] Generated default text: {}", defaultText);
            return defaultText;
        }
    }
    
    /**
     * Check if the transcribed text is mock data.
     */
    public boolean isMockTranscription(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // Check for common mock text patterns
        String lowerText = text.toLowerCase();
        return lowerText.contains("hello friends") || 
               lowerText.contains("नमस्ते दोस्तों") ||
               lowerText.contains("hola amigos") ||
               lowerText.contains("bonjour les amis") ||
               lowerText.contains("hallo freunde") ||
               lowerText.contains("mock transcription");
    }
    
    /**
     * Upload audio file to S3 for transcription.
     */
    private void uploadAudioToS3(File audioFile, String s3Key) throws IOException {
        logger.info("[S3 Upload] Uploading audio file to S3: {}", s3Key);
        logger.info("[S3 Upload] File size: {} bytes", audioFile.length());
        logger.info("[S3 Upload] Bucket: {}", bucketName);
        logger.info("[S3 Upload] File path: {}", audioFile.getAbsolutePath());
        
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("audio/mpeg")
                    .build();
            
            logger.info("[S3 Upload] PutObjectRequest created: bucket={}, key={}", bucketName, s3Key);
            
            RequestBody requestBody = RequestBody.fromFile(audioFile);
            var putObjectResponse = s3Client.putObject(putObjectRequest, requestBody);
            
            logger.info("[S3 Upload] Successfully uploaded audio file to S3: {}", s3Key);
            logger.info("[S3 Upload] Upload response ETag: {}", putObjectResponse.eTag());
            logger.info("[S3 Upload] Upload response version ID: {}", putObjectResponse.versionId());
        } catch (Exception e) {
            logger.error("[S3 Upload] Failed to upload audio file to S3: {}", e.getMessage());
            logger.error("[S3 Upload] Exception type: {}", e.getClass().getSimpleName());
            throw new IOException("Failed to upload audio file to S3", e);
        }
    }
    
    /**
     * Wait for transcription job to complete and return the transcribed text.
     */
    private String waitForTranscriptionCompletion(String jobName) throws Exception {
        int maxAttempts = 60; // 5 minutes with 5-second intervals
        int attempt = 0;
        
        logger.info("[Transcription Wait] Waiting for transcription job completion: {}", jobName);
        
        while (attempt < maxAttempts) {
            GetTranscriptionJobRequest request = GetTranscriptionJobRequest.builder()
                    .transcriptionJobName(jobName)
                    .build();
            
            GetTranscriptionJobResponse response = transcribeClient.getTranscriptionJob(request);
            TranscriptionJob job = response.transcriptionJob();
            
            logger.info("[Transcription Wait] Job status: {} (attempt {}/{})", job.transcriptionJobStatus(), attempt + 1, maxAttempts);
            
            if (TranscriptionJobStatus.COMPLETED.equals(job.transcriptionJobStatus())) {
                logger.info("[Transcription Wait] Job completed successfully!");
                logger.info("[Transcription Wait] Transcript URI: {}", job.transcript().transcriptFileUri());
                
                // Wait a moment for the result file to be fully written
                logger.info("[Transcription Wait] Waiting 3 seconds for result file to be fully written...");
                Thread.sleep(3000);
                
                // Download and parse the transcription result
                return downloadAndParseTranscriptionResult(job.transcript().transcriptFileUri());
            } else if (TranscriptionJobStatus.FAILED.equals(job.transcriptionJobStatus())) {
                logger.error("[Transcription Wait] Job failed: {}", job.failureReason());
                throw new Exception("Transcription job failed: " + job.failureReason());
            }
            
            Thread.sleep(5000); // Wait 5 seconds before checking again
            attempt++;
        }
        
        throw new Exception("Transcription job timed out after 5 minutes");
    }
    
    /**
     * Download and parse transcription result from S3.
     */
               private String downloadAndParseTranscriptionResult(String transcriptUri) throws Exception {
               logger.info("[Transcription Result] Downloading transcription result from: {}", transcriptUri);
               
               // Extract S3 key from URI - handle different URI formats
               String s3Key;
               logger.info("[Transcription Result] Parsing transcript URI: {}", transcriptUri);
               
               // Log the URI analysis for debugging
               logger.info("[Transcription Result] URI analysis:");
               logger.info("[Transcription Result] - Contains '.s3.amazonaws.com/': {}", transcriptUri.contains(".s3.amazonaws.com/"));
               logger.info("[Transcription Result] - Contains '.s3.': {}", transcriptUri.contains(".s3."));
               logger.info("[Transcription Result] - Contains bucket name: {}", transcriptUri.contains(bucketName));
               
               if (transcriptUri.contains(".s3.amazonaws.com/")) {
                   // Handle format: https://bucket.s3.amazonaws.com/key (virtual-hosted style)
                   s3Key = transcriptUri.replace("https://" + bucketName + ".s3.amazonaws.com/", "");
                   logger.info("[Transcription Result] Extracted key (virtual-hosted format): {}", s3Key);
               } else if (transcriptUri.contains("s3.")) {
                   // Handle region-specific URLs like https://s3.us-east-1.amazonaws.com/bucket/key (path-style)
                   // Format: https://s3.us-east-1.amazonaws.com/bucket/key
                   // Extract the S3 key by finding the bucket name and taking everything after it
                   
                   // First, try to find the bucket name in the URI
                   String[] uriParts = transcriptUri.split("/");
                   logger.info("[Transcription Result] URI parts count: {}", uriParts.length);
                   for (int i = 0; i < uriParts.length; i++) {
                       logger.info("[Transcription Result] Part {}: '{}'", i, uriParts[i]);
                   }
                   
                   // Find the bucket name in the parts
                   int bucketIndex = -1;
                   for (int i = 0; i < uriParts.length; i++) {
                       if (bucketName.equals(uriParts[i])) {
                           bucketIndex = i;
                           break;
                       }
                   }
                   
                   logger.info("[Transcription Result] Bucket '{}' found at index: {}", bucketName, bucketIndex);
                   
                   if (bucketIndex != -1 && bucketIndex + 1 < uriParts.length) {
                       // Build the S3 key from parts after the bucket name
                       StringBuilder keyBuilder = new StringBuilder();
                       for (int i = bucketIndex + 1; i < uriParts.length; i++) {
                           if (i > bucketIndex + 1) keyBuilder.append("/");
                           keyBuilder.append(uriParts[i]);
                       }
                       s3Key = keyBuilder.toString();
                       logger.info("[Transcription Result] Extracted key (path-style format): {}", s3Key);
                       
                       // Validate the extracted key
                       if (s3Key == null || s3Key.trim().isEmpty()) {
                           logger.error("[Transcription Result] Extracted key is empty from URI: {}", transcriptUri);
                           throw new Exception("Extracted S3 key is empty from transcript URI: " + transcriptUri);
                       }
                       
                       // Verify the key format looks correct (should contain transcription-results/)
                       if (!s3Key.startsWith("transcription-results/")) {
                           logger.warn("[Transcription Result] Extracted key doesn't start with 'transcription-results/': {}", s3Key);
                       }
                   } else {
                       logger.error("[Transcription Result] Could not find bucket name or key in URI: {}", transcriptUri);
                       throw new Exception("Could not find bucket name or key in transcript URI: " + transcriptUri);
                   }
               } else {
                   logger.error("[Transcription Result] Unsupported URI format: {}", transcriptUri);
                   throw new Exception("Unsupported transcript URI format: " + transcriptUri);
               }
        
                       logger.info("[Transcription Result] Extracted S3 key: {}", s3Key);
               logger.info("[Transcription Result] Bucket: {}", bucketName);
               
               // Test the extraction logic with the actual URI format
               String testUri = "https://s3.us-east-1.amazonaws.com/video-translation-bucket2/transcription-results/transcription-887dc1e1.json";
               String testBucketPattern = "/" + bucketName + "/";
               int testBucketIndex = testUri.indexOf(testBucketPattern);
               if (testBucketIndex != -1) {
                   String testKey = testUri.substring(testBucketIndex + testBucketPattern.length());
                   logger.info("[Transcription Result] Test extraction - URI: {}", testUri);
                   logger.info("[Transcription Result] Test extraction - Pattern: {}", testBucketPattern);
                   logger.info("[Transcription Result] Test extraction - Index: {}", testBucketIndex);
                   logger.info("[Transcription Result] Test extraction - Key: {}", testKey);
               }
        
        // Verify the result file exists before downloading
        logger.info("[Transcription Result] Verifying result file exists...");
        try {
            var headObjectResponse = s3Client.headObject(builder -> builder.bucket(bucketName).key(s3Key));
            logger.info("[Transcription Result] Result file exists, size: {} bytes", headObjectResponse.contentLength());
        } catch (Exception e) {
            logger.error("[Transcription Result] Result file not found: {}", e.getMessage());
            throw new Exception("Transcription result file not found: " + s3Key, e);
        }
        
        // Download the JSON result file directly to memory instead of temp file
        logger.info("[Transcription Result] Downloading transcription result from S3...");
        try (var response = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(s3Key).build())) {
            String jsonContent = new String(response.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            logger.info("[Transcription Result] Downloaded content length: {} characters", jsonContent.length());
            logger.info("[Transcription Result] Content preview: {}", jsonContent.substring(0, Math.min(200, jsonContent.length())));
            
            // Parse JSON to extract transcribed text - more robust parsing
            logger.info("[Transcription Result] Parsing JSON response...");
            
            // First, try to find the transcript in the results.transcripts array
            if (jsonContent.contains("\"results\"")) {
                logger.info("[Transcription Result] Found 'results' field in JSON");
                
                // Look for the transcript in the results structure
                if (jsonContent.contains("\"transcripts\"")) {
                    logger.info("[Transcription Result] Found 'transcripts' array in JSON");
                    
                    // Find the transcript field within the transcripts array
                    int transcriptsStart = jsonContent.indexOf("\"transcripts\"");
                    if (transcriptsStart != -1) {
                        // Look for the transcript field after transcripts
                        int transcriptFieldStart = jsonContent.indexOf("\"transcript\":", transcriptsStart);
                        if (transcriptFieldStart != -1) {
                            // Find the start of the transcript value
                            int valueStart = jsonContent.indexOf("\"", transcriptFieldStart + 13) + 1;
                            if (valueStart > 0) {
                                // Find the end of the transcript value (handle escaped quotes)
                                int valueEnd = valueStart;
                                boolean inEscape = false;
                                for (int i = valueStart; i < jsonContent.length(); i++) {
                                    char c = jsonContent.charAt(i);
                                    if (c == '\\' && !inEscape) {
                                        inEscape = true;
                                    } else if (c == '"' && !inEscape) {
                                        valueEnd = i;
                                        break;
                                    } else {
                                        inEscape = false;
                                    }
                                }
                                
                                if (valueEnd > valueStart) {
                                    String transcript = jsonContent.substring(valueStart, valueEnd);
                                    // Unescape the transcript
                                    transcript = transcript.replace("\\\"", "\"").replace("\\\\", "\\");
                                    logger.info("[Transcription Result] SUCCESS: Transcription completed: {}", transcript);
                                    return transcript;
                                }
                            }
                        }
                    }
                }
            }
            
            // Fallback: try the simple approach
            if (jsonContent.contains("\"transcript\":")) {
                logger.info("[Transcription Result] Using fallback parsing method");
                int start = jsonContent.indexOf("\"transcript\":") + 13;
                int end = jsonContent.indexOf("\"", start);
                if (end > start) {
                    String transcript = jsonContent.substring(start, end).trim();
                    // Unescape the transcript
                    transcript = transcript.replace("\\\"", "\"").replace("\\\\", "\\");
                    logger.info("[Transcription Result] SUCCESS: Transcription completed (fallback): {}", transcript);
                    return transcript;
                }
            }
            
            // If we get here, we couldn't parse the transcript
            logger.error("[Transcription Result] Failed to extract transcript from JSON");
            logger.error("[Transcription Result] JSON structure analysis:");
            logger.error("[Transcription Result] - Contains 'results': {}", jsonContent.contains("\"results\""));
            logger.error("[Transcription Result] - Contains 'transcripts': {}", jsonContent.contains("\"transcripts\""));
            logger.error("[Transcription Result] - Contains 'transcript': {}", jsonContent.contains("\"transcript\""));
            logger.error("[Transcription Result] - Full JSON content: {}", jsonContent);
            throw new Exception("Failed to extract transcript from JSON response - JSON structure not recognized");
        } catch (Exception e) {
            logger.error("[Transcription Result] Failed to download or parse transcription result: {}", e.getMessage());
            throw new Exception("Failed to download or parse transcription result: " + e.getMessage(), e);
        }
    }
    
    /**
     * Clean up transcription files from S3.
     */
    private void cleanupTranscriptionFiles(String audioS3Key, String jobName) {
        try {
            // Delete audio file
            s3Client.deleteObject(builder -> builder.bucket(bucketName).key(audioS3Key));
            
            // Delete transcription result
            String resultKey = "transcription-results/" + jobName + ".json";
            s3Client.deleteObject(builder -> builder.bucket(bucketName).key(resultKey));
            
            logger.debug("Cleaned up transcription files");
        } catch (Exception e) {
            logger.warn("Failed to cleanup transcription files: {}", e.getMessage());
        }
    }
    
    /**
     * Verify S3 bucket access on startup.
     */
    private void verifyS3BucketAccess() {
        try {
            var headBucketResponse = s3Client.headBucket(builder -> builder.bucket(bucketName));
            logger.info("[S3 Bucket] Successfully verified access to bucket: {}", bucketName);
            
            var bucketRegionList = headBucketResponse.sdkHttpResponse().headers().get("x-amz-bucket-region");
            String bucketRegion = bucketRegionList != null && !bucketRegionList.isEmpty() ? bucketRegionList.get(0) : null;
            logger.info("[S3 Bucket] Bucket region: {}", bucketRegion);
            logger.info("[S3 Bucket] Configured region: {}", awsRegion);
            
            if (bucketRegion != null && !bucketRegion.equals(awsRegion)) {
                logger.warn("[S3 Bucket] WARNING: Bucket region ({}) differs from configured region ({})", bucketRegion, awsRegion);
                logger.warn("[S3 Bucket] This might cause AWS Transcribe to fail accessing the bucket");
            } else {
                logger.info("[S3 Bucket] Region configuration is correct");
            }
        } catch (Exception e) {
            logger.error("[S3 Bucket] Failed to access bucket {}: {}", bucketName, e.getMessage());
            logger.error("[S3 Bucket] Please check your AWS credentials and bucket permissions");
        }
    }
    
    /**
     * Check for existing transcription jobs to avoid conflicts.
     */
    private void checkExistingTranscriptionJobs() {
        try {
            var listJobsResponse = transcribeClient.listTranscriptionJobs(builder -> builder.maxResults(10));
            int activeJobs = 0;
            
            for (var job : listJobsResponse.transcriptionJobSummaries()) {
                if ("IN_PROGRESS".equals(job.transcriptionJobStatusAsString()) || 
                    "QUEUED".equals(job.transcriptionJobStatusAsString())) {
                    activeJobs++;
                    logger.info("[Transcription Jobs] Active job found: {} (status: {})", 
                               job.transcriptionJobName(), job.transcriptionJobStatusAsString());
                }
            }
            
            logger.info("[Transcription Jobs] Found {} active transcription jobs", activeJobs);
            
            if (activeJobs > 5) {
                logger.warn("[Transcription Jobs] WARNING: Many active transcription jobs detected. This might cause delays.");
            }
            
        } catch (Exception e) {
            logger.warn("[Transcription Jobs] Failed to check existing transcription jobs: {}", e.getMessage());
        }
    }
    
    /**
     * Test S3 URI format specifically for AWS Transcribe.
     */
    private void testS3UriForTranscribe(String s3Uri, String s3Key) throws Exception {
        logger.info("[S3 URI Test] Testing S3 URI format for AWS Transcribe: {}", s3Uri);
        
        try {
            // Test 1: Verify the URI format
            if (!s3Uri.startsWith("s3://")) {
                throw new Exception("S3 URI must start with 's3://'");
            }
            
            // Test 2: Verify bucket name in URI
            String expectedBucket = s3Uri.substring(5, s3Uri.indexOf("/", 5));
            if (!expectedBucket.equals(bucketName)) {
                throw new Exception("Bucket name in URI does not match configured bucket");
            }
            
            // Test 3: Verify key in URI
            String expectedKey = s3Uri.substring(s3Uri.indexOf("/", 5) + 1);
            if (!expectedKey.equals(s3Key)) {
                throw new Exception("Key in URI does not match expected key");
            }
            
            logger.info("[S3 URI Test] S3 URI format is correct");
            logger.info("[S3 URI Test] Bucket: {}", expectedBucket);
            logger.info("[S3 URI Test] Key: {}", expectedKey);
            
            // Test 4: Verify the file is accessible using the same S3 client
            logger.info("[S3 URI Test] Verifying file accessibility...");
            var headObjectResponse = s3Client.headObject(builder -> builder.bucket(bucketName).key(s3Key));
            logger.info("[S3 URI Test] File is accessible, size: {} bytes", headObjectResponse.contentLength());
            
        } catch (Exception e) {
            logger.error("[S3 URI Test] S3 URI test failed: {}", e.getMessage());
            throw new Exception("S3 URI test failed for transcription: " + s3Uri, e);
        }
    }
    
    /**
     * Verify S3 URI is accessible for transcription.
     */
    private void verifyS3UriAccess(String s3Uri, String s3Key) throws Exception {
        logger.info("[S3 URI Verification] Verifying S3 URI accessibility: {}", s3Uri);
        
        try {
            // Try to get object using the same S3 client
            var getObjectResponse = s3Client.getObject(builder -> builder.bucket(bucketName).key(s3Key));
            logger.info("[S3 URI Verification] S3 URI is accessible");
            logger.info("[S3 URI Verification] Object content length: {}", getObjectResponse.response().contentLength());
            logger.info("[S3 URI Verification] Object content type: {}", getObjectResponse.response().contentType());
            
            // Additional verification: check if the file is actually readable
            logger.info("[S3 URI Verification] Testing file readability...");
            var inputStream = getObjectResponse;
            byte[] buffer = new byte[1024];
            int bytesRead = inputStream.read(buffer);
            logger.info("[S3 URI Verification] Successfully read {} bytes from file", bytesRead);
            
        } catch (Exception e) {
            logger.error("[S3 URI Verification] S3 URI is not accessible: {}", e.getMessage());
            throw new Exception("S3 URI is not accessible for transcription: " + s3Uri, e);
        }
    }
    
    /**
     * Verify that a file exists in S3 before starting transcription.
     */
    private void verifyS3FileExists(String s3Key) throws Exception {
        int maxRetries = 10;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // Try to get object metadata to verify it exists
                var headObjectResponse = s3Client.headObject(builder -> builder.bucket(bucketName).key(s3Key));
                logger.info("[S3 Verification] File exists in S3: {}", s3Key);
                logger.info("[S3 Verification] File size: {} bytes", headObjectResponse.contentLength());
                logger.info("[S3 Verification] File ETag: {}", headObjectResponse.eTag());
                logger.info("[S3 Verification] File last modified: {}", headObjectResponse.lastModified());
                
                // Additional verification: try to actually read a small portion of the file
                logger.info("[S3 Verification] Testing file read access...");
                var getObjectResponse = s3Client.getObject(builder -> builder.bucket(bucketName).key(s3Key).range("bytes=0-1023"));
                logger.info("[S3 Verification] File read test successful");
                
                return;
            } catch (Exception e) {
                retryCount++;
                logger.warn("[S3 Verification] File not found in S3 (attempt {}/{}): {}", retryCount, maxRetries, s3Key);
                logger.warn("[S3 Verification] Error: {}", e.getMessage());
                
                if (retryCount >= maxRetries) {
                    throw new Exception("File not found in S3 after " + maxRetries + " attempts: " + s3Key);
                }
                
                // Wait longer before retrying
                Thread.sleep(3000);
            }
        }
    }
    
    /**
     * Get language code for transcription.
     */
    public String getLanguageCode(String language) {
        switch (language.toLowerCase()) {
            case "hindi":
            case "hi":
                return "hi-IN";
            case "english":
            case "en":
                return "en-US";
            case "spanish":
            case "es":
                return "es-US";
            case "french":
            case "fr":
                return "fr-FR";
            case "german":
            case "de":
                return "de-DE";
            case "chinese":
            case "zh":
                return "zh-CN";
            case "japanese":
            case "ja":
                return "ja-JP";
            case "korean":
            case "ko":
                return "ko-KR";
            default:
                return "en-US";
        }
    }
    
    /**
     * Transcribe audio from S3 key to text using AWS Transcribe.
     * This method works entirely with S3 and avoids local file operations.
     */
    public String transcribeAudioFromS3(String s3Key, String languageCode) throws Exception {
        logger.info("[Transcription] Starting transcription for S3 key: {}", s3Key);
        
        try {
            // Verify the file exists in S3 before starting transcription
            logger.info("[Transcription] Verifying S3 file exists before starting transcription...");
            verifyS3FileExists(s3Key);
            
            // Add a longer delay to ensure S3 consistency
            logger.info("[Transcription] Waiting 10 seconds for S3 consistency...");
            Thread.sleep(10000);
            
            // Start transcription job
            String jobName = "transcription-" + UUID.randomUUID().toString().substring(0, 8);
            String s3Uri = "s3://" + bucketName + "/" + s3Key;
            
            // Check for existing transcription jobs to avoid conflicts
            logger.info("[Transcription] Checking for existing transcription jobs...");
            checkExistingTranscriptionJobs();
            
            // Double-check the S3 URI format
            logger.info("[Transcription] S3 URI for transcription: {}", s3Uri);
            logger.info("[Transcription] S3 URI components - bucket: {}, key: {}", bucketName, s3Key);
            
            // Verify the S3 URI is accessible
            verifyS3UriAccess(s3Uri, s3Key);
            
            // Final verification: one more check right before starting transcription
            logger.info("[Transcription] Final S3 verification before starting transcription...");
            Thread.sleep(2000);
            verifyS3FileExists(s3Key);
            
            // Test the exact S3 URI format that AWS Transcribe expects
            logger.info("[Transcription] Testing S3 URI format for AWS Transcribe...");
            testS3UriForTranscribe(s3Uri, s3Key);
            
            logger.info("[Transcription] Starting transcription job: {} with S3 URI: {}", jobName, s3Uri);
            logger.info("[Transcription] Creating transcription job with parameters:");
            logger.info("[Transcription] - Job name: {}", jobName);
            logger.info("[Transcription] - S3 URI: {}", s3Uri);
            logger.info("[Transcription] - Language code: {}", languageCode);
            logger.info("[Transcription] - Output bucket: {}", bucketName);
            logger.info("[Transcription] - Output key: transcription-results/{}.json", jobName);
            
            StartTranscriptionJobRequest request = StartTranscriptionJobRequest.builder()
                    .transcriptionJobName(jobName)
                    .media(Media.builder().mediaFileUri(s3Uri).build())
                    .languageCode(languageCode)
                    .outputBucketName(bucketName)
                    .outputKey("transcription-results/" + jobName + ".json")
                    .build();
            
            try {
                logger.info("[Transcription] Starting transcription job with request: {}", request);
                StartTranscriptionJobResponse response = transcribeClient.startTranscriptionJob(request);
                logger.info("[Transcription] Transcription job started successfully: {}", jobName);
                logger.info("[Transcription] Job status: {}", response.transcriptionJob().transcriptionJobStatus());
                logger.info("[Transcription] Job name: {}", response.transcriptionJob().transcriptionJobName());
            } catch (Exception e) {
                logger.error("[Transcription] Failed to start transcription job: {}", e.getMessage());
                logger.error("[Transcription] S3 URI used: {}", s3Uri);
                logger.error("[Transcription] Bucket name: {}", bucketName);
                logger.error("[Transcription] Language code: {}", languageCode);
                logger.error("[Transcription] Exception type: {}", e.getClass().getSimpleName());
                logger.error("[Transcription] Full exception details:", e);
                
                // Check for specific "invalid media file" error
                if (e.getMessage().contains("isn't valid") || e.getMessage().contains("invalid media file")) {
                    logger.error("[Transcription] INVALID MEDIA FILE ERROR DETECTED");
                    logger.error("[Transcription] This usually means:");
                    logger.error("[Transcription] 1. Audio file is corrupted or has no audio content");
                    logger.error("[Transcription] 2. Audio format is not supported by AWS Transcribe");
                    logger.error("[Transcription] 3. Audio file is too short or too long");
                    logger.error("[Transcription] 4. Audio file has invalid encoding");
                    
                    // Try to get more information about the file
                    try {
                        logger.info("[Transcription] Attempting to analyze the problematic file...");
                        analyzeProblematicFile(s3Key);
                    } catch (Exception analysisError) {
                        logger.error("[Transcription] File analysis failed: {}", analysisError.getMessage());
                    }
                    
                    throw new Exception("Invalid media file detected. Please check that your audio file contains valid audio content and is in a supported format (MP3, WAV, FLAC, M4A). Error: " + e.getMessage(), e);
                }
                
                // Try to verify the file one more time after the error
                try {
                    logger.info("[Transcription] Re-verifying S3 file after error...");
                    verifyS3FileExists(s3Key);
                    logger.info("[Transcription] File still exists in S3 after error");
                } catch (Exception verifyError) {
                    logger.error("[Transcription] File verification failed after error: {}", verifyError.getMessage());
                }
                
                throw e;
            }
            
            // Wait for transcription to complete
            logger.info("[Transcription] Waiting for transcription to complete...");
            String transcribedText = waitForTranscriptionCompletion(jobName);
            
            logger.info("[Transcription] SUCCESS: Transcription completed!");
            logger.info("[Transcription] Transcribed text ({} chars): {}", transcribedText.length(), transcribedText);
            
            // Clean up S3 files
            cleanupTranscriptionFiles(s3Key, jobName);
            
            return transcribedText;
            
        } catch (Exception e) {
            logger.error("AWS Transcribe failed with error: {}", e.getMessage());
            logger.error("S3 key: {}, languageCode: {}", s3Key, languageCode);
            logger.error("Stack trace:", e);
            
            // Don't use mock transcription - throw the error instead
            logger.error("AWS Transcribe failed. Please check your AWS credentials and permissions.");
            logger.error("Required permissions: transcribe:StartTranscriptionJob, transcribe:GetTranscriptionJob, s3:PutObject, s3:GetObject");
            throw new Exception("AWS Transcribe failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate audio file before transcription.
     * 
     * @param audioFile The audio file to validate
     * @throws Exception if validation fails
     */
    private void validateAudioFile(File audioFile) throws Exception {
        logger.info("[Audio Validation] Starting validation for: {}", audioFile.getName());
        
        // Check if file exists
        if (!audioFile.exists()) {
            throw new Exception("Audio file does not exist: " + audioFile.getAbsolutePath());
        }
        
        // Check if file is readable
        if (!audioFile.canRead()) {
            throw new Exception("Audio file is not readable: " + audioFile.getAbsolutePath());
        }
        
        // Check file size
        long fileSize = audioFile.length();
        logger.info("[Audio Validation] File size: {} bytes", fileSize);
        
        if (fileSize == 0) {
            throw new Exception("Audio file is empty: " + audioFile.getAbsolutePath());
        }
        
        if (fileSize < 1024) {
            logger.warn("[Audio Validation] File is very small ({} bytes), may contain no audio", fileSize);
        }
        
        if (fileSize > 100 * 1024 * 1024) { // 100MB limit
            throw new Exception("Audio file is too large (" + fileSize + " bytes). Maximum size is 100MB");
        }
        
        // Check file extension
        String fileName = audioFile.getName().toLowerCase();
        if (!fileName.endsWith(".mp3") && !fileName.endsWith(".wav") && 
            !fileName.endsWith(".flac") && !fileName.endsWith(".m4a") && 
            !fileName.endsWith(".mp4") && !fileName.endsWith(".avi") && 
            !fileName.endsWith(".mov") && !fileName.endsWith(".mkv")) {
            throw new Exception("Unsupported audio/video format: " + fileName + ". Supported formats: mp3, wav, flac, m4a, mp4, avi, mov, mkv");
        }
        
        // Try to validate audio content using FFmpeg
        validateAudioContent(audioFile);
        
        logger.info("[Audio Validation] File validation passed: {}", audioFile.getName());
    }
    
    /**
     * Validate audio content using FFmpeg.
     * 
     * @param audioFile The audio file to validate
     * @throws Exception if validation fails
     */
    private void validateAudioContent(File audioFile) throws Exception {
        try {
            logger.info("[Audio Validation] Validating audio content with FFmpeg");
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                "C:\\ffmpeg\\bin\\ffprobe.exe",
                "-v", "quiet",
                "-show_entries", "format=duration,size",
                "-show_streams",
                "-select_streams", "a:0",
                "-of", "json",
                audioFile.getAbsolutePath()
            );
            
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes());
            String errorOutput = new String(process.getErrorStream().readAllBytes());
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                logger.error("[Audio Validation] FFprobe failed with exit code: {}", exitCode);
                logger.error("[Audio Validation] Error output: {}", errorOutput);
                throw new Exception("Invalid audio file: FFprobe validation failed. Error: " + errorOutput);
            }
            
            // Check if output contains audio stream information
            if (!output.contains("\"codec_type\":\"audio\"")) {
                throw new Exception("No audio stream found in file. File may be video-only or corrupted.");
            }
            
            // Check duration
            if (output.contains("\"duration\":")) {
                String durationStr = output.split("\"duration\":\"")[1].split("\"")[0];
                try {
                    double duration = Double.parseDouble(durationStr);
                    logger.info("[Audio Validation] Audio duration: {} seconds", duration);
                    
                    if (duration < 0.5) {
                        throw new Exception("Audio duration too short (" + duration + " seconds). Minimum duration is 0.5 seconds.");
                    }
                    
                    if (duration > 3600) { // 1 hour limit
                        throw new Exception("Audio duration too long (" + duration + " seconds). Maximum duration is 1 hour.");
                    }
                } catch (NumberFormatException e) {
                    logger.warn("[Audio Validation] Could not parse duration: {}", durationStr);
                }
            }
            
            logger.info("[Audio Validation] Audio content validation passed");
            
        } catch (Exception e) {
            if (e.getMessage().contains("Invalid audio file")) {
                throw e; // Re-throw our custom validation errors
            }
            logger.warn("[Audio Validation] FFprobe validation failed, but continuing: {}", e.getMessage());
            // Don't throw here - FFprobe might not be available, but we can still try transcription
        }
    }
    
    /**
     * Convert audio file to a compatible format for AWS Transcribe.
     * 
     * @param audioFile The original audio file
     * @return The converted audio file
     * @throws Exception if conversion fails
     */
    private File convertAudioForTranscription(File audioFile) throws Exception {
        logger.info("[Audio Conversion] Converting audio for AWS Transcribe compatibility");
        
        // Create temporary file for converted audio
        File convertedFile = File.createTempFile("converted_", ".mp3");
        convertedFile.deleteOnExit();
        
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "C:\\ffmpeg\\bin\\ffmpeg.exe",
                "-i", audioFile.getAbsolutePath(),
                "-acodec", "mp3",
                "-ar", "16000",  // 16kHz sample rate
                "-ac", "1",      // Mono channel
                "-b:a", "128k",  // 128kbps bitrate
                "-y",            // Overwrite output file
                convertedFile.getAbsolutePath()
            );
            
            logger.info("[Audio Conversion] FFmpeg command: {}", String.join(" ", processBuilder.command()));
            
            Process process = processBuilder.start();
            
            // Capture error output
            StringBuilder errorOutput = new StringBuilder();
            Thread errorReader = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = process.getErrorStream().read(buffer)) != -1) {
                        errorOutput.append(new String(buffer, 0, bytesRead));
                    }
                } catch (IOException e) {
                    logger.warn("[Audio Conversion] Error reading FFmpeg error stream: {}", e.getMessage());
                }
            });
            errorReader.start();
            
            // Wait for completion
            boolean completed = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!completed) {
                logger.error("[Audio Conversion] Audio conversion timed out");
                process.destroyForcibly();
                errorReader.interrupt();
                throw new Exception("Audio conversion timed out");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("[Audio Conversion] FFmpeg failed with exit code: {}", exitCode);
                logger.error("[Audio Conversion] Error output: {}", errorOutput.toString());
                throw new Exception("Audio conversion failed: " + errorOutput.toString());
            }
            
            // Verify converted file
            if (!convertedFile.exists() || convertedFile.length() == 0) {
                throw new Exception("Converted audio file was not created or is empty");
            }
            
            logger.info("[Audio Conversion] Audio conversion completed successfully");
            logger.info("[Audio Conversion] Original: {} bytes, Converted: {} bytes", 
                       audioFile.length(), convertedFile.length());
            
            return convertedFile;
            
        } catch (Exception e) {
            // Clean up converted file if it exists
            if (convertedFile.exists()) {
                convertedFile.delete();
            }
            throw e;
        }
    }

    /**
     * Analyze a problematic audio file to help diagnose issues.
     * 
     * @param s3Key The S3 key of the problematic file
     * @throws Exception if analysis fails
     */
    private void analyzeProblematicFile(String s3Key) throws Exception {
        logger.info("[File Analysis] Analyzing problematic file: {}", s3Key);
        
        try {
            // Download the file temporarily for analysis
            File tempFile = File.createTempFile("analysis_", ".tmp");
            tempFile.deleteOnExit();
            
            try {
                // Download from S3
                logger.info("[File Analysis] Downloading file from S3 for analysis...");
                var response = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(s3Key).build());
                Files.copy(response, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                logger.info("[File Analysis] File downloaded: {} bytes", tempFile.length());
                
                // Analyze with FFprobe
                ProcessBuilder processBuilder = new ProcessBuilder(
                    "C:\\ffmpeg\\bin\\ffprobe.exe",
                    "-v", "error",
                    "-show_entries", "format=duration,size,format_name",
                    "-show_streams",
                    "-of", "json",
                    tempFile.getAbsolutePath()
                );
                
                Process process = processBuilder.start();
                String output = new String(process.getInputStream().readAllBytes());
                String errorOutput = new String(process.getErrorStream().readAllBytes());
                int exitCode = process.waitFor();
                
                logger.info("[File Analysis] FFprobe analysis results:");
                logger.info("[File Analysis] Exit code: {}", exitCode);
                logger.info("[File Analysis] Output: {}", output);
                if (!errorOutput.isEmpty()) {
                    logger.error("[File Analysis] Error output: {}", errorOutput);
                }
                
                // Parse the JSON output for key information
                if (output.contains("\"streams\"")) {
                    logger.info("[File Analysis] File contains streams");
                    
                    // Check for audio streams
                    if (output.contains("\"codec_type\":\"audio\"")) {
                        logger.info("[File Analysis] Audio stream found");
                        
                        // Extract audio codec
                        if (output.contains("\"codec_name\":")) {
                            String codecName = output.split("\"codec_name\":\"")[1].split("\"")[0];
                            logger.info("[File Analysis] Audio codec: {}", codecName);
                        }
                        
                        // Extract sample rate
                        if (output.contains("\"sample_rate\":")) {
                            String sampleRate = output.split("\"sample_rate\":\"")[1].split("\"")[0];
                            logger.info("[File Analysis] Sample rate: {} Hz", sampleRate);
                        }
                        
                        // Extract channels
                        if (output.contains("\"channels\":")) {
                            String channels = output.split("\"channels\":")[1].split(",")[0];
                            logger.info("[File Analysis] Channels: {}", channels);
                        }
                    } else {
                        logger.error("[File Analysis] NO AUDIO STREAM FOUND - This is likely the problem!");
                    }
                    
                    // Check for video streams
                    if (output.contains("\"codec_type\":\"video\"")) {
                        logger.info("[File Analysis] Video stream found");
                    }
                } else {
                    logger.error("[File Analysis] No streams found in file - file may be corrupted");
                }
                
                // Check duration
                if (output.contains("\"duration\":")) {
                    String durationStr = output.split("\"duration\":\"")[1].split("\"")[0];
                    try {
                        double duration = Double.parseDouble(durationStr);
                        logger.info("[File Analysis] Duration: {} seconds", duration);
                        
                        if (duration < 0.5) {
                            logger.error("[File Analysis] DURATION TOO SHORT - This may cause transcription failure");
                        }
                        
                        if (duration > 3600) {
                            logger.error("[File Analysis] DURATION TOO LONG - This may cause transcription failure");
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("[File Analysis] Could not parse duration: {}", durationStr);
                    }
                }
                
                // Check file size
                if (output.contains("\"size\":")) {
                    String sizeStr = output.split("\"size\":\"")[1].split("\"")[0];
                    try {
                        long size = Long.parseLong(sizeStr);
                        logger.info("[File Analysis] File size: {} bytes", size);
                        
                        if (size < 1024) {
                            logger.error("[File Analysis] FILE TOO SMALL - This may indicate no audio content");
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("[File Analysis] Could not parse size: {}", sizeStr);
                    }
                }
                
            } finally {
                // Clean up temporary file
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
            
        } catch (Exception e) {
            logger.error("[File Analysis] Analysis failed: {}", e.getMessage());
            throw e;
        }
    }
} 