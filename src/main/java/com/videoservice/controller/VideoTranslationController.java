package com.videoservice.controller;

import com.videoservice.dto.TranslationRequest;
import com.videoservice.dto.TranslationResponse;
import com.videoservice.dto.TranslationResultResponse;
import com.videoservice.model.JobStatus;
import com.videoservice.model.TranslationJob;
import com.videoservice.model.TranslationResult;
import com.videoservice.model.TranslationStatus;
import com.videoservice.repository.TranslationJobRepository;
import com.videoservice.service.JobManager;
import com.videoservice.service.S3StorageService;
import com.videoservice.service.LocalStorageService;
import com.videoservice.service.VideoProcessingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for video translation operations.
 * Handles file uploads, translation requests, and job management.
 */
@RestController
@RequestMapping("/translation")
@CrossOrigin(origins = "*")
public class VideoTranslationController {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoTranslationController.class);
    
    private final TranslationJobRepository jobRepository;
    private final JobManager jobManager;
    private final S3StorageService s3StorageService;
    private final LocalStorageService localStorageService;
    private final VideoProcessingService videoProcessingService;
    
    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;
    
    @Value("${video.max-duration}")
    private long maxDurationSeconds;
    
    public VideoTranslationController(TranslationJobRepository jobRepository,
                                    JobManager jobManager,
                                    S3StorageService s3StorageService,
                                    LocalStorageService localStorageService,
                                    VideoProcessingService videoProcessingService) {
        this.jobRepository = jobRepository;
        this.jobManager = jobManager;
        this.s3StorageService = s3StorageService;
        this.localStorageService = localStorageService;
        this.videoProcessingService = videoProcessingService;
    }
    
    /**
     * Upload video file and start translation job.
     * 
     * @param file The video file to upload
     * @param request The translation request parameters
     * @return Response with job information
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranslationResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute TranslationRequest request) {
        
        logger.info("Received video upload request: {} for languages: {}", 
                   file.getOriginalFilename(), request.getTargetLanguages());
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("File is empty"));
            }
            
            // Save file temporarily
            Path tempFile = Files.createTempFile("upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile.toFile());
            
            // Validate video format and duration
            try {
                var videoInfo = videoProcessingService.validateVideo(tempFile.toFile());
                if (videoInfo.getDurationSeconds() > maxDurationSeconds) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Video duration exceeds maximum allowed duration"));
                }
            } catch (IOException e) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid video file: " + e.getMessage()));
            }
            
            // Try to upload to S3 first, fallback to local storage if S3 fails
            String storageKey;
            try {
                storageKey = s3StorageService.uploadFile(tempFile.toFile(), file.getOriginalFilename());
                logger.info("Successfully uploaded to S3: {}", storageKey);
            } catch (Exception s3Error) {
                logger.warn("S3 upload failed, falling back to local storage: {}", s3Error.getMessage());
                storageKey = localStorageService.uploadFile(tempFile.toFile(), file.getOriginalFilename());
                logger.info("Successfully uploaded to local storage: {}", storageKey);
            }
            
            // Create translation job
            TranslationJob job = new TranslationJob(
                    file.getOriginalFilename(),
                    request.getSourceLanguage(),
                    request.getTargetLanguages(),
                    request.getUserEmail()
            );
            job.setS3OriginalKey(storageKey);
            job.setFileSizeBytes(file.getSize());
            
            // Save job to database
            job = jobRepository.save(job);
            
            // Start processing asynchronously
            jobManager.processTranslationJob(job);
            
            // Clean up temporary file
            Files.deleteIfExists(tempFile);
            
            TranslationResponse response = createTranslationResponse(job);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            
        } catch (Exception e) {
            logger.error("Failed to process video upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to process video upload: " + e.getMessage()));
        }
    }
    
    /**
     * Get job status by ID.
     * 
     * @param jobId The job ID
     * @return Job status and details
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<TranslationResponse> getJobStatus(@PathVariable UUID jobId) {
        logger.debug("Getting job status for: {}", jobId);
        
        return jobRepository.findById(jobId)
                .map(job -> ResponseEntity.ok(createTranslationResponse(job)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get jobs for a user.
     * 
     * @param userEmail The user email
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Page of user's jobs
     */
    @GetMapping("/jobs")
    public ResponseEntity<Page<TranslationResponse>> getUserJobs(
            @RequestParam String userEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.debug("Getting jobs for user: {}", userEmail);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TranslationJob> jobs = jobRepository.findByUserEmailOrderByCreatedAtDesc(userEmail, pageable);
        
        Page<TranslationResponse> responses = jobs.map(this::createTranslationResponse);
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get jobs by status.
     * 
     * @param status The job status
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Page of jobs with the given status
     */
    @GetMapping("/jobs/status/{status}")
    public ResponseEntity<Page<TranslationResponse>> getJobsByStatus(
            @PathVariable JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.debug("Getting jobs with status: {}", status);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TranslationJob> jobs = jobRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        
        Page<TranslationResponse> responses = jobs.map(this::createTranslationResponse);
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Cancel a running job.
     * 
     * @param jobId The job ID to cancel
     * @return Success response
     */
    @PostMapping("/job/{jobId}/cancel")
    public ResponseEntity<Map<String, String>> cancelJob(@PathVariable UUID jobId) {
        logger.info("Cancelling job: {}", jobId);
        
        return jobRepository.findById(jobId)
                .map(job -> {
                    if (job.getStatus().isInProgress()) {
                        job.setStatus(JobStatus.CANCELLED);
                        jobRepository.save(job);
                        return ResponseEntity.ok(Map.of("message", "Job cancelled successfully"));
                    } else {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Job cannot be cancelled in its current state"));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Retry a failed job.
     * 
     * @param jobId The job ID to retry
     * @return Success response
     */
    @PostMapping("/job/{jobId}/retry")
    public ResponseEntity<Map<String, String>> retryJob(@PathVariable UUID jobId) {
        logger.info("Retrying job: {}", jobId);
        
        return jobRepository.findById(jobId)
                .map(job -> {
                    if (job.getStatus() == JobStatus.FAILED) {
                        job.setStatus(JobStatus.PENDING);
                        job.setErrorMessage(null);
                        job.setProgressPercentage(0);
                        jobRepository.save(job);
                        
                        // Start processing again
                        jobManager.processTranslationJob(job);
                        
                        return ResponseEntity.ok(Map.of("message", "Job retry started successfully"));
                    } else {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Job cannot be retried in its current state"));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get download URL for a translated video.
     * 
     * @param jobId The job ID
     * @param language The target language
     * @return Download URL
     */
    @GetMapping("/job/{jobId}/download/{language}")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable UUID jobId,
            @PathVariable String language) {
        
        logger.debug("Getting download URL for job: {} language: {}", jobId, language);
        
        return jobRepository.findById(jobId)
                .map(job -> {
                    if (job.getStatus() == JobStatus.COMPLETED) {
                        // Find the translation result for the language
                        TranslationResult result = job.getTranslationResults().stream()
                                .filter(r -> r.getTargetLanguage().equalsIgnoreCase(language))
                                .findFirst()
                                .orElse(null);
                        
                        if (result != null && result.getS3VideoKey() != null) {
                            try {
                                String downloadUrl = s3StorageService.getPresignedDownloadUrl(result.getS3VideoKey(), 60); // 1 hour
                                return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
                            } catch (Exception e) {
                                logger.error("Failed to generate download URL: {}", e.getMessage());
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", "Failed to generate download URL"));
                            }
                        } else {
                            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                    .body(Map.of("error", "Translation result not found"));
                        }
                    } else {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Job is not completed"));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get system statistics.
     * 
     * @return System statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        logger.debug("Getting system statistics");
        
        Map<String, Object> stats = jobManager.getSystemStats();
        
        // Add job counts
        stats.put("totalJobs", jobRepository.count());
        stats.put("pendingJobs", jobRepository.countByStatus(JobStatus.PENDING));
        stats.put("processingJobs", jobRepository.countByStatus(JobStatus.PROCESSING));
        stats.put("completedJobs", jobRepository.countByStatus(JobStatus.COMPLETED));
        stats.put("failedJobs", jobRepository.countByStatus(JobStatus.FAILED));
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get supported languages.
     * 
     * @return List of supported languages
     */
    @GetMapping("/languages")
    public ResponseEntity<List<String>> getSupportedLanguages() {
        List<String> languages = List.of("english", "arabic", "korean", "chinese", "tamil", "hindi");
        return ResponseEntity.ok(languages);
    }
    
    /**
     * Health check endpoint.
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "Video Translation Service"));
    }
    
    /**
     * Create translation response from job entity.
     * 
     * @param job The translation job
     * @return Translation response
     */
    private TranslationResponse createTranslationResponse(TranslationJob job) {
        TranslationResponse response = new TranslationResponse(job.getId(), job.getOriginalFilename(), job.getStatus());
        response.setSourceLanguage(job.getSourceLanguage());
        response.setTargetLanguages(job.getTargetLanguages());
        response.setUserEmail(job.getUserEmail());
        response.setProgressPercentage(job.getProgressPercentage());
        response.setFileSizeBytes(job.getFileSizeBytes());
        response.setDurationSeconds(job.getDurationSeconds());
        response.setErrorMessage(job.getErrorMessage());
        response.setCreatedAt(job.getCreatedAt());
        response.setUpdatedAt(job.getUpdatedAt());
        response.setCompletedAt(job.getCompletedAt());
        
        // Add translation results
        if (job.getTranslationResults() != null) {
            List<TranslationResultResponse> resultResponses = job.getTranslationResults().stream()
                    .map(this::createResultResponse)
                    .collect(Collectors.toList());
            response.setResults(resultResponses);
        }
        
        return response;
    }
    
    /**
     * Create translation result response from result entity.
     * 
     * @param result The translation result
     * @return Translation result response
     */
    private TranslationResultResponse createResultResponse(TranslationResult result) {
        TranslationResultResponse response = new TranslationResultResponse(
                result.getId(), result.getTargetLanguage(), result.getStatus());
        response.setS3VideoKey(result.getS3VideoKey());
        response.setTranslationQualityScore(result.getTranslationQualityScore());
        response.setAudioQualityScore(result.getAudioQualityScore());
        response.setProcessingTimeSeconds(result.getProcessingTimeSeconds());
        response.setFileSizeBytes(result.getFileSizeBytes());
        response.setErrorMessage(result.getErrorMessage());
        response.setCreatedAt(result.getCreatedAt());
        response.setCompletedAt(result.getCompletedAt());
        
        // Generate download URL if completed
        if (result.getStatus() == TranslationStatus.COMPLETED && result.getS3VideoKey() != null) {
            try {
                String downloadUrl = s3StorageService.getPresignedDownloadUrl(result.getS3VideoKey(), 60);
                response.setDownloadUrl(downloadUrl);
            } catch (Exception e) {
                logger.warn("Failed to generate download URL for result: {}", result.getId());
            }
        }
        
        return response;
    }
    
    /**
     * Create error response.
     * 
     * @param errorMessage The error message
     * @return Error response
     */
    private TranslationResponse createErrorResponse(String errorMessage) {
        TranslationResponse response = new TranslationResponse();
        response.setErrorMessage(errorMessage);
        response.setStatus(JobStatus.FAILED);
        return response;
    }
} 