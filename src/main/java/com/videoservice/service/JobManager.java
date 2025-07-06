package com.videoservice.service;

import com.videoservice.dto.TranslationRequest;
import com.videoservice.model.JobStatus;
import com.videoservice.model.TranslationJob;
import com.videoservice.model.TranslationResult;
import com.videoservice.model.TranslationStatus;
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
    private final VideoProcessingService videoProcessingService;
    private final TranslationService translationService;
    private final AudioSynthesisService audioSynthesisService;
    private final NotificationService notificationService;
    
    @Value("${audio.chunk-duration:180}")
    private int chunkDurationSeconds;
    
    @Value("${job.max-concurrent-jobs:5}")
    private int maxConcurrentJobs;
    
    @Value("${job.retry-attempts:3}")
    private int retryAttempts;
    
    @Value("${job.retry-delay:5000}")
    private long retryDelayMs;
    
    public JobManager(S3StorageService s3StorageService,
                     VideoProcessingService videoProcessingService,
                     TranslationService translationService,
                     AudioSynthesisService audioSynthesisService,
                     NotificationService notificationService) {
        this.s3StorageService = s3StorageService;
        this.videoProcessingService = videoProcessingService;
        this.translationService = translationService;
        this.audioSynthesisService = audioSynthesisService;
        this.notificationService = notificationService;
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
            
            // Send job started notification
            notificationService.sendJobStartedNotification(job);
            
            // Download video from S3
            Path tempDir = Files.createTempDirectory("translation_" + job.getId());
            File videoFile = s3StorageService.downloadFile(job.getS3OriginalKey(), tempDir.resolve("original.mp4"));
            
            // Validate video
            var videoInfo = videoProcessingService.validateVideo(videoFile);
            job.setDurationSeconds(videoInfo.getDurationSeconds());
            job.setFileSizeBytes(videoInfo.getFileSizeBytes());
            job.updateProgress(20);
            
            // Extract audio from video
            File audioFile = tempDir.resolve("audio.mp3").toFile();
            videoProcessingService.extractAudio(videoFile, audioFile);
            job.updateProgress(30);
            
            // Split audio into chunks
            Path chunksDir = tempDir.resolve("chunks");
            Files.createDirectories(chunksDir);
            List<File> audioChunks = videoProcessingService.splitAudioIntoChunks(audioFile, chunkDurationSeconds, chunksDir);
            job.updateProgress(40);
            
            // Process each target language
            for (String targetLanguage : job.getTargetLanguages()) {
                processTargetLanguage(job, targetLanguage, audioChunks, tempDir);
            }
            
            // Mark job as completed
            job.markAsCompleted();
            
            // Generate download URLs and send completion notification
            Map<String, String> downloadUrls = notificationService.generateDownloadUrls(job);
            notificationService.sendJobCompletionNotification(job, downloadUrls);
            
            logger.info("Translation job completed successfully: {}", job.getId());
            
        } catch (Exception e) {
            logger.error("Translation job failed: {} - {}", job.getId(), e.getMessage());
            handleJobFailure(job, e);
        }
        
        return CompletableFuture.completedFuture(null);
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
                
                // Convert audio to text (this would require speech-to-text service)
                String transcribedText = transcribeAudio(audioChunk);
                
                // Translate text
                String translatedText = translationService.translateText(
                    transcribedText, job.getSourceLanguage(), targetLanguage);
                
                // Synthesize speech from translated text
                File translatedAudioChunk = tempDir.resolve(
                    String.format("translated_%s_chunk_%03d.mp3", targetLanguage, i)).toFile();
                
                audioSynthesisService.synthesizeSpeech(translatedText, targetLanguage, translatedAudioChunk);
                translatedAudioChunks.add(translatedAudioChunk);
                
                // Update progress
                int progress = 40 + (i + 1) * 50 / (audioChunks.size() * job.getTargetLanguages().size());
                job.updateProgress(progress);
            }
            
            // Merge translated audio chunks
            File mergedAudio = tempDir.resolve("merged_" + targetLanguage + ".mp3").toFile();
            videoProcessingService.mergeAudioChunks(translatedAudioChunks, mergedAudio);
            
            // Replace audio in original video
            File outputVideo = tempDir.resolve("output_" + targetLanguage + ".mp4").toFile();
            videoProcessingService.replaceAudioInVideo(
                tempDir.resolve("original.mp4").toFile(), mergedAudio, outputVideo);
            
            // Upload to S3
            String s3Key = s3StorageService.generateTranslatedVideoKey(job.getOriginalFilename(), targetLanguage);
            s3StorageService.uploadFileWithKey(outputVideo, s3Key);
            
            // Update result
            result.setS3VideoKey(s3Key);
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
     * Transcribe audio to text (placeholder implementation).
     * In a real implementation, this would use AWS Transcribe or similar service.
     * 
     * @param audioFile The audio file to transcribe
     * @return The transcribed text
     * @throws Exception if transcription fails
     */
    private String transcribeAudio(File audioFile) throws Exception {
        // This is a placeholder implementation
        // In a real system, you would use AWS Transcribe or similar service
        logger.debug("Transcribing audio file: {}", audioFile.getName());
        
        // Simulate transcription delay
        Thread.sleep(1000);
        
        // Return placeholder text
        return "This is a placeholder transcription. In a real implementation, this would be the actual transcribed text from the audio file.";
    }
    
    /**
     * Handle job failure.
     * 
     * @param job The failed job
     * @param exception The exception that caused the failure
     */
    private void handleJobFailure(TranslationJob job, Exception exception) {
        job.markAsFailed(exception.getMessage());
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