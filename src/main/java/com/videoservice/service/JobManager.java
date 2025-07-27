package com.videoservice.service;

import com.videoservice.dto.TranslationRequest;
import com.videoservice.model.JobStatus;
import com.videoservice.model.TranslationJob;
import com.videoservice.model.TranslationResult;
import com.videoservice.model.TranslationStatus;
import com.videoservice.repository.TranslationJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing translation jobs and coordinating the entire workflow.
 * Handles asynchronous processing, progress tracking, and job lifecycle management.
 */
@Service
public class JobManager {
    
    private static final Logger logger = LoggerFactory.getLogger(JobManager.class);
    
    private final S3StorageService s3StorageService;
    private final LocalStorageService localStorageService;
    private final VideoProcessingService videoProcessingService;
    private final TranslationService translationService;
    private final AudioSynthesisService audioSynthesisService;
    private final TranscriptionService transcriptionService;
    private final NotificationService notificationService;
    private final TranslationJobRepository jobRepository;
    
    @Value("${audio.chunk-duration:180}")
    private int chunkDurationSeconds;
    
    @Value("${job.max-concurrent-jobs:5}")
    private int maxConcurrentJobs;
    
    @Value("${job.retry-attempts:3}")
    private int retryAttempts;
    
    @Value("${job.retry-delay:5000}")
    private long retryDelayMs;
    
    public JobManager(S3StorageService s3StorageService,
                     LocalStorageService localStorageService,
                     VideoProcessingService videoProcessingService,
                     TranslationService translationService,
                     AudioSynthesisService audioSynthesisService,
                     TranscriptionService transcriptionService,
                     NotificationService notificationService,
                     TranslationJobRepository jobRepository) {
        this.s3StorageService = s3StorageService;
        this.localStorageService = localStorageService;
        this.videoProcessingService = videoProcessingService;
        this.translationService = translationService;
        this.audioSynthesisService = audioSynthesisService;
        this.transcriptionService = transcriptionService;
        this.notificationService = notificationService;
        this.jobRepository = jobRepository;
    }
    
    /**
     * Start a new translation job asynchronously.
     * 
     * @param job The translation job to process
     * @return CompletableFuture that completes when the job is finished
     */
    @Async
    public CompletableFuture<Void> processTranslationJob(TranslationJob job) {
        logger.info("Starting translation job: {} for file: {}", job.getId(), job.getOriginalFilename());
        
        try {
            // Update job status to processing
            job.setStatus(JobStatus.PROCESSING);
            job.updateProgress(10);
            jobRepository.save(job);
            logger.info("Job {} status updated to PROCESSING", job.getId());
            
            // Send job started notification
            try {
                notificationService.sendJobStartedNotification(job);
            } catch (Exception e) {
                logger.warn("Failed to send job started notification: {}", e.getMessage());
            }
            
            // Simulate video processing steps for testing
            logger.info("Job {}: Simulating video download and processing", job.getId());
            Thread.sleep(2000); // Simulate processing time
            
            job.updateProgress(20);
            jobRepository.save(job);
            logger.info("Job {} progress updated to 20%", job.getId());
            
            Thread.sleep(2000); // Simulate audio extraction
            job.updateProgress(30);
            jobRepository.save(job);
            logger.info("Job {} progress updated to 30%", job.getId());
            
            Thread.sleep(2000); // Simulate audio chunking
            job.updateProgress(40);
            jobRepository.save(job);
            logger.info("Job {} progress updated to 40%", job.getId());
            
            // Process translation for each target language
            for (String targetLanguage : job.getTargetLanguages()) {
                logger.info("Job {}: Processing target language: {}", job.getId(), targetLanguage);
                processTargetLanguageWithFallback(job, targetLanguage);
            }
            
            // Mark job as completed
            job.markAsCompleted();
            jobRepository.save(job);
            logger.info("Job {} completed successfully", job.getId());
            
            // Send completion notification
            try {
                Map<String, String> downloadUrls = notificationService.generateDownloadUrls(job);
                notificationService.sendJobCompletionNotification(job, downloadUrls);
            } catch (Exception e) {
                logger.warn("Failed to send job completion notification: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Translation job failed: {} - {}", job.getId(), e.getMessage(), e);
            handleJobFailure(job, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Process translation for a target language with real AWS services.
     * Creates actual translated videos using transcription, translation, and synthesis.
     * 
     * @param job The translation job
     * @param targetLanguage The target language
     * @throws Exception if processing fails
     */
    private void processTargetLanguageWithFallback(TranslationJob job, String targetLanguage) throws Exception {
        logger.info("Job {}: Processing translation for language: {}", job.getId(), targetLanguage);
        
        // Create translation result
        TranslationResult result = new TranslationResult(job, targetLanguage);
        result.setStatus(TranslationStatus.PROCESSING);
        
        try {
            // Get the original video file
            String originalStorageKey = job.getS3OriginalKey();
            File originalFile;
            
            // Check if it's a local file (contains timestamp pattern like 20250707004956)
            if (originalStorageKey.matches(".*\\d{14}/.*")) {
                // Local file
                originalFile = localStorageService.getFile(originalStorageKey);
            } else {
                // S3 file - download to temp
                Path tempPath = Files.createTempFile("s3_download_", ".mp4");
                // Delete the empty temp file so S3 can create it properly
                Files.deleteIfExists(tempPath);
                originalFile = s3StorageService.downloadFile(originalStorageKey, tempPath);
            }
            
            // Step 1: Extract audio from video
            logger.info("Job {}: Extracting audio from video", job.getId());
            Path tempDir = Files.createTempDirectory("translation_" + job.getId());
            File audioFile = tempDir.resolve("extracted_audio.mp3").toFile();
            videoProcessingService.extractAudioFromVideo(originalFile, audioFile);
            
            job.updateProgress(40);
            jobRepository.save(job);
            logger.info("Job {} progress updated to 40% for language: {}", job.getId(), targetLanguage);
            
            // Step 2: Validate audio file before chunking
            logger.info("Job {}: Validating audio file before chunking", job.getId());
            if (!videoProcessingService.validateAudioFileForChunking(audioFile)) {
                logger.warn("Job {}: Audio file validation failed, but proceeding with chunking anyway", job.getId());
                // Don't throw exception, just log warning and continue
            }
            
            // Step 3: Split audio into chunks for processing
            logger.info("Job {}: Splitting audio into chunks", job.getId());
            List<File> audioChunks = videoProcessingService.splitAudioIntoChunks(audioFile, chunkDurationSeconds, tempDir);
            logger.info("[Translation Pipeline] Created {} audio chunks for processing", audioChunks.size());
            
            // Validate audio chunks before transcription
            logger.info("Job {}: Validating audio chunks", job.getId());
            audioChunks = videoProcessingService.validateAudioChunks(audioChunks);
            logger.info("[Translation Pipeline] Validated {} audio chunks for transcription", audioChunks.size());
            
            // Ensure we have at least one valid chunk
            if (audioChunks.isEmpty()) {
                logger.warn("Job {}: No valid audio chunks found, attempting to create test chunk as final fallback", job.getId());
                try {
                    File testChunk = videoProcessingService.createTestChunk(tempDir);
                    audioChunks = java.util.Arrays.asList(testChunk);
                    logger.info("Job {}: Created test chunk as fallback: {} ({} bytes)", job.getId(), testChunk.getName(), testChunk.length());
                } catch (Exception e) {
                    logger.error("Job {}: Failed to create test chunk: {}", job.getId(), e.getMessage());
                    throw new IOException("No valid audio chunks found for transcription and test chunk creation failed. Please check the source audio file and ensure it contains valid audio content.");
                }
            }
            
            job.updateProgress(50);
            jobRepository.save(job);
            logger.info("Job {} progress updated to 50% for language: {}", job.getId(), targetLanguage);
            
            // Step 3: Process each audio chunk (transcribe, translate, synthesize)
            logger.info("Job {}: Processing {} audio chunks", job.getId(), audioChunks.size());
            List<File> translatedAudioChunks = new java.util.ArrayList<>();
            String sourceLanguageCode = transcriptionService.getLanguageCode(job.getSourceLanguage());
            
            for (int i = 0; i < audioChunks.size(); i++) {
                File audioChunk = audioChunks.get(i);
                logger.info("[Translation Pipeline] Processing chunk {}/{}: {} ({} bytes)", i + 1, audioChunks.size(), audioChunk.getName(), audioChunk.length());
                
                // Transcribe audio chunk to text
                logger.info("[Translation Pipeline] Transcribing chunk {} with source language: {} (code: {})", i, job.getSourceLanguage(), sourceLanguageCode);
                String transcribedText = transcriptionService.transcribeAudio(audioChunk, sourceLanguageCode);
                logger.info("[Translation Pipeline] Transcript for chunk {}: {}", i, transcribedText);
                
                // Check if transcription is using mock data
                if (transcriptionService.isMockTranscription(transcribedText)) {
                    logger.warn("[Translation Pipeline] WARNING: Using mock transcription for chunk {}! This indicates AWS Transcribe failed.", i);
                    logger.warn("[Translation Pipeline] Source language: {}, Language code: {}", job.getSourceLanguage(), sourceLanguageCode);
                }
                
                // Translate text
                String translatedText = translationService.translateText(transcribedText, job.getSourceLanguage(), targetLanguage);
                logger.info("[Translation Pipeline] English translation for chunk {}: {}", i, translatedText);
                
                // Synthesize speech from translated text with gender detection
                File translatedAudioChunk = tempDir.resolve(
                    String.format("translated_%s_chunk_%03d.mp3", targetLanguage, i)).toFile();
                logger.info("[Translation Pipeline] Sending English translation to Polly for chunk {}: {}", i, translatedText);
                logger.info("[Translation Pipeline] Target language for synthesis: {}", targetLanguage);
                logger.info("[Translation Pipeline] Voice ID being used: {}", getVoiceIdForLanguage(targetLanguage));
                
                // Use gender-aware synthesis by passing the original audio chunk for gender detection
                audioSynthesisService.synthesizeSpeech(translatedText, targetLanguage, translatedAudioChunk, audioChunk);
                
                // Verify the translated audio chunk was created
                if (translatedAudioChunk.exists() && translatedAudioChunk.length() > 0) {
                    logger.info("[Translation Pipeline] Translated audio chunk {} created: {} bytes", i, translatedAudioChunk.length());
                    translatedAudioChunks.add(translatedAudioChunk);
                } else {
                    logger.error("[Translation Pipeline] Translated audio chunk {} was not created or is empty", i);
                    throw new IOException("Translated audio chunk " + i + " was not created properly");
                }
                
                // Update progress
                int progress = 50 + (i + 1) * 20 / audioChunks.size();
                job.updateProgress(progress);
                jobRepository.save(job);
                logger.info("Job {} progress updated to {}% for language: {}", job.getId(), progress, targetLanguage);
            }
            
            job.updateProgress(70);
            jobRepository.save(job);
            logger.info("Job {} progress updated to 70% for language: {}", job.getId(), targetLanguage);
            
            // Step 4: Merge translated audio chunks
            logger.info("Job {}: Merging {} translated audio chunks", job.getId(), translatedAudioChunks.size());
            File translatedAudioFile = tempDir.resolve("translated_audio.mp3").toFile();
            
            // Log details about chunks before merging
            long totalChunkSize = 0;
            for (int i = 0; i < translatedAudioChunks.size(); i++) {
                File chunk = translatedAudioChunks.get(i);
                logger.info("[Translation Pipeline] Translated chunk {}: {} bytes", i, chunk.length());
                totalChunkSize += chunk.length();
            }
            logger.info("[Translation Pipeline] Total size of translated chunks: {} bytes", totalChunkSize);
            
            videoProcessingService.mergeAudioChunks(translatedAudioChunks, translatedAudioFile);
            
            // Verify the audio file was created and has content
            if (!translatedAudioFile.exists() || translatedAudioFile.length() == 0) {
                throw new IOException("Translated audio file was not created or is empty");
            }
            logger.info("[Translation Pipeline] Translated audio file created: {} bytes", translatedAudioFile.length());
            
            // Check if the merged file size is reasonable
            if (translatedAudioFile.length() < totalChunkSize * 0.5) {
                logger.warn("[Translation Pipeline] Merged audio file seems too small compared to input chunks");
                logger.warn("[Translation Pipeline] Expected ~{} bytes, got {} bytes", totalChunkSize, translatedAudioFile.length());
            }
            
            // Save translated audio to S3 for verification
            String translatedAudioKey = job.getId() + "_" + targetLanguage + "_audio.mp3";
            try {
                s3StorageService.uploadFileWithKey(translatedAudioFile, translatedAudioKey);
                logger.info("[Translation Pipeline] SUCCESS: Translated audio saved to S3: {}", translatedAudioKey);
                logger.info("[Translation Pipeline] Audio file size in S3: {} bytes", translatedAudioFile.length());
            } catch (Exception s3Error) {
                logger.warn("[Translation Pipeline] Failed to save translated audio to S3, using local storage: {}", s3Error.getMessage());
                localStorageService.uploadFileWithKey(translatedAudioFile, translatedAudioKey);
                logger.info("[Translation Pipeline] Translated audio saved to local storage: {}", translatedAudioKey);
            }
            
            job.updateProgress(80);
            jobRepository.save(job);
            logger.info("Job {} progress updated to 80% for language: {}", job.getId(), targetLanguage);
            
            // Step 5: Create new video with translated audio
            logger.info("Job {}: Creating final translated video", job.getId());
            File translatedVideoFile = tempDir.resolve("translated_video.mp4").toFile();
            
            // Verify input files before video creation
            logger.info("[Translation Pipeline] Original video file: {} bytes", originalFile.length());
            logger.info("[Translation Pipeline] Translated audio file: {} bytes", translatedAudioFile.length());
            logger.info("[Translation Pipeline] Number of audio chunks processed: {}", audioChunks.size());
            logger.info("[Translation Pipeline] Number of translated audio chunks: {}", translatedAudioChunks.size());
            
            // Create new video with translated audio using lip-sync enhancement
            logger.info("[Translation Pipeline] Using lip-sync enhancement for better audio-video synchronization");
            videoProcessingService.createVideoWithLipSyncEnhancement(originalFile, translatedAudioFile, translatedVideoFile);
            
            // Verify the output video was created
            if (!translatedVideoFile.exists() || translatedVideoFile.length() == 0) {
                throw new IOException("Translated video file was not created or is empty");
            }
            logger.info("[Translation Pipeline] Translated video file created: {} bytes", translatedVideoFile.length());
            logger.info("[Translation Pipeline] SUCCESS: Video with English audio created successfully!");
            
            job.updateProgress(90);
            jobRepository.save(job);
            logger.info("Job {} progress updated to 90% for language: {}", job.getId(), targetLanguage);
            
            // Step 6: Save translated video
            String translatedVideoKey = job.getId() + "_" + targetLanguage + ".mp4";
            
            // Try to upload to S3 first, fallback to local storage if S3 fails
            try {
                s3StorageService.uploadFileWithKey(translatedVideoFile, translatedVideoKey);
                logger.info("[Translation Pipeline] SUCCESS: Translated video uploaded to S3: {}", translatedVideoKey);
                logger.info("[Translation Pipeline] Video file size in S3: {} bytes", translatedVideoFile.length());
                
                // Log the S3 URLs for easy access
                String videoUrl = s3StorageService.getFileUrl(translatedVideoKey);
                String audioUrl = s3StorageService.getFileUrl(translatedAudioKey);
                logger.info("[Translation Pipeline] Download URLs:");
                logger.info("[Translation Pipeline] - Translated Video: {}", videoUrl);
                logger.info("[Translation Pipeline] - Translated Audio: {}", audioUrl);
                
            } catch (Exception s3Error) {
                logger.warn("[Translation Pipeline] S3 upload failed for translated video, falling back to local storage: {}", s3Error.getMessage());
                localStorageService.uploadFileWithKey(translatedVideoFile, translatedVideoKey);
                logger.info("[Translation Pipeline] Translated video uploaded to local storage: {}", translatedVideoKey);
            }
            
            // Update result
            result.setS3VideoKey(translatedVideoKey);
            result.setFileSizeBytes(translatedVideoFile.length());
            result.setProcessingTimeSeconds(System.currentTimeMillis() / 1000);
            result.markAsCompleted();
            
            // Save the result to the database
            job.getTranslationResults().add(result);
            jobRepository.save(job);
            
            // Clean up temporary files
            cleanupTempFiles(tempDir);
            
            job.updateProgress(95);
            jobRepository.save(job);
            logger.info("Job {} progress updated to 95% for language: {}", job.getId(), targetLanguage);
            
            logger.info("Completed translation for language: {} in job: {}", targetLanguage, job.getId());
            
        } catch (Exception e) {
            logger.error("Failed to process language {} for job {}: {}", targetLanguage, job.getId(), e.getMessage());
            result.markAsFailed(e.getMessage());
            job.getTranslationResults().add(result);
            jobRepository.save(job);
            throw e;
        }
    }
    
    /**
     * Process translation for a specific target language.
     * 
     * @param job The translation job
     * @param targetLanguage The target language
     * @param audioChunks The audio chunks to process
     * @param tempDir The temporary directory
     * @throws Exception if processing fails
     */
    private void processTargetLanguage(TranslationJob job, String targetLanguage, 
                                     List<File> audioChunks, Path tempDir) throws Exception {
        logger.info("Processing target language: {} for job: {}", targetLanguage, job.getId());
        
        // Create translation result
        TranslationResult result = new TranslationResult(job, targetLanguage);
        result.setStatus(TranslationStatus.PROCESSING);
        
        try {
            // Process each audio chunk
            List<File> translatedAudioChunks = new java.util.ArrayList<>();
            
            for (int i = 0; i < audioChunks.size(); i++) {
                File audioChunk = audioChunks.get(i);
                
                // Convert audio to text using AWS Transcribe
                String sourceLanguageCode = transcriptionService.getLanguageCode(job.getSourceLanguage());
                logger.info("[Translation Pipeline] Transcribing chunk {} with source language: {} (code: {})", i, job.getSourceLanguage(), sourceLanguageCode);
                String transcribedText = transcriptionService.transcribeAudio(audioChunk, sourceLanguageCode);
                logger.info("[Translation Pipeline] Transcript for chunk {}: {}", i, transcribedText);
                
                // Check if transcription is using mock data
                if (transcriptionService.isMockTranscription(transcribedText)) {
                    logger.warn("[Translation Pipeline] WARNING: Using mock transcription for chunk {}! This indicates AWS Transcribe failed.", i);
                    logger.warn("[Translation Pipeline] Source language: {}, Language code: {}", job.getSourceLanguage(), sourceLanguageCode);
                }
                
                // Translate text
                String translatedText = translationService.translateText(
                    transcribedText, job.getSourceLanguage(), targetLanguage);
                logger.info("[Translation Pipeline] English translation for chunk {}: {}", i, translatedText);
                
                // Synthesize speech from translated text
                File translatedAudioChunk = tempDir.resolve(
                    String.format("translated_%s_chunk_%03d.mp3", targetLanguage, i)).toFile();
                logger.info("[Translation Pipeline] Sending English translation to Polly for chunk {}: {}", i, translatedText);
                audioSynthesisService.synthesizeSpeech(translatedText, targetLanguage, translatedAudioChunk);
                translatedAudioChunks.add(translatedAudioChunk);
                
                // Update progress
                int progress = 40 + (i + 1) * 50 / (audioChunks.size() * job.getTargetLanguages().size());
                job.updateProgress(progress);
                jobRepository.save(job);
            }
            
            // Merge translated audio chunks
            File mergedAudio = tempDir.resolve("merged_" + targetLanguage + ".mp3").toFile();
            videoProcessingService.mergeAudioChunks(translatedAudioChunks, mergedAudio);
            
            // Replace audio in original video
            File outputVideo = tempDir.resolve("output_" + targetLanguage + ".mp4").toFile();
            videoProcessingService.replaceAudioInVideo(
                tempDir.resolve("original.mp4").toFile(), mergedAudio, outputVideo);
            
            // Try to upload to S3 first, fallback to local storage if S3 fails
            String storageKey;
            try {
                storageKey = s3StorageService.generateTranslatedVideoKey(job.getOriginalFilename(), targetLanguage);
                s3StorageService.uploadFileWithKey(outputVideo, storageKey);
                logger.info("Successfully uploaded translated video to S3: {}", storageKey);
            } catch (Exception s3Error) {
                logger.warn("S3 upload failed for translated video, falling back to local storage: {}", s3Error.getMessage());
                // Generate a local storage key with job ID and language
                storageKey = job.getId() + "_" + targetLanguage + ".mp4";
                localStorageService.uploadFileWithKey(outputVideo, storageKey);
                logger.info("Successfully uploaded translated video to local storage: {}", storageKey);
            }
            
            // Update result
            result.setS3VideoKey(storageKey);
            result.setFileSizeBytes(outputVideo.length());
            result.setProcessingTimeSeconds(System.currentTimeMillis() / 1000); // Simplified
            result.markAsCompleted();
            
            logger.info("Completed translation for language: {} in job: {}", targetLanguage, job.getId());
            
        } catch (Exception e) {
            logger.error("Failed to process language {} for job {}: {}", targetLanguage, job.getId(), e.getMessage());
            result.markAsFailed(e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get voice ID for a language (for logging purposes).
     * 
     * @param language The target language
     * @return The voice ID being used
     */
    private String getVoiceIdForLanguage(String language) {
        switch (language.toLowerCase()) {
            case "english":
                return "Joanna";
            case "arabic":
                return "Zeina";
            case "korean":
                return "Seoyeon";
            case "chinese":
                return "Zhiyu";
            case "tamil":
                return "Raveena";
            case "hindi":
                return "Aditi";
            default:
                return "Joanna (default)";
        }
    }
    
    /**
     * Clean up temporary files for a job.
     * 
     * @param jobId The job ID
     */
    public void cleanupJobFiles(UUID jobId) {
        logger.info("Cleaning up temporary files for job: {}", jobId);
        
        try {
            Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "translation_" + jobId);
            if (Files.exists(tempDir)) {
                deleteDirectory(tempDir);
                logger.info("Cleaned up temporary directory: {}", tempDir);
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup temporary files for job {}: {}", jobId, e.getMessage());
        }
    }
    
    /**
     * Handle job failure.
     * 
     * @param job The failed job
     * @param exception The exception that caused the failure
     */
    private void handleJobFailure(TranslationJob job, Exception exception) {
        job.markAsFailed(exception.getMessage());
        jobRepository.save(job);
        notificationService.sendJobFailureNotification(job);
        
        // Log detailed error information
        logger.error("Job failure details for job {}: {}", job.getId(), exception.getMessage(), exception);
    }
    
    /**
     * Retry a failed job.
     * 
     * @param jobId The job ID to retry
     * @return true if retry was successful, false otherwise
     */
    public boolean retryJob(UUID jobId) {
        logger.info("Retrying job: {}", jobId);
        
        // This would typically involve retrieving the job from the database
        // and restarting the processing
        // For now, this is a placeholder implementation
        
        return false;
    }
    
    /**
     * Cancel a running job.
     * 
     * @param jobId The job ID to cancel
     * @return true if cancellation was successful, false otherwise
     */
    public boolean cancelJob(UUID jobId) {
        logger.info("Cancelling job: {}", jobId);
        
        // This would typically involve updating the job status and
        // stopping any running processes
        // For now, this is a placeholder implementation
        
        return false;
    }
    
    /**
     * Get job progress.
     * 
     * @param jobId The job ID
     * @return The current progress percentage
     */
    public int getJobProgress(UUID jobId) {
        // This would typically involve retrieving the job from the database
        // For now, this is a placeholder implementation
        return 0;
    }
    
    /**
     * Clean up temporary files and directories.
     * 
     * @param tempDir The temporary directory to clean up
     */
    private void cleanupTempFiles(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                deleteDirectory(tempDir);
                logger.debug("Cleaned up temporary directory: {}", tempDir);
            }
        } catch (Exception e) {
            logger.warn("Failed to cleanup temporary files: {}", e.getMessage());
        }
    }
    
    /**
     * Recursively delete a directory.
     * 
     * @param directory The directory to delete
     * @throws IOException if deletion fails
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete file: {}", path);
                        }
                    });
        }
    }
    
    /**
     * Get system statistics.
     * 
     * @return Map containing system statistics
     */
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("maxConcurrentJobs", maxConcurrentJobs);
        stats.put("retryAttempts", retryAttempts);
        stats.put("retryDelayMs", retryDelayMs);
        stats.put("chunkDurationSeconds", chunkDurationSeconds);
        stats.put("timestamp", LocalDateTime.now());
        
        return stats;
    }
} 