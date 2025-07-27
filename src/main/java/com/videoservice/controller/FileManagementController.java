package com.videoservice.controller;

import com.videoservice.dto.FileUploadRequest;
import com.videoservice.dto.FileUploadResponse;
import com.videoservice.dto.TranslationJobRequest;
import com.videoservice.dto.TranslationResponse;
import com.videoservice.model.TranslationJob;
import com.videoservice.model.JobStatus;
import com.videoservice.repository.TranslationJobRepository;
import com.videoservice.service.FileManagementService;
import com.videoservice.service.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Controller for file management operations.
 * Handles file upload, listing, and translation job creation from existing files.
 */
@RestController
@RequestMapping("/api/v1/files")
@CrossOrigin(origins = "*")
public class FileManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileManagementController.class);
    
    private final FileManagementService fileManagementService;
    private final JobManager jobManager;
    private final TranslationJobRepository jobRepository;
    
    @Autowired
    public FileManagementController(FileManagementService fileManagementService, JobManager jobManager, TranslationJobRepository jobRepository) {
        this.fileManagementService = fileManagementService;
        this.jobManager = jobManager;
        this.jobRepository = jobRepository;
    }
    
    /**
     * Upload a video file to S3.
     * 
     * @param file The video file to upload
     * @param userEmail The user's email
     * @return File upload response with metadata
     */
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userEmail") String userEmail) {
        
        logger.info("[File Management] Upload request for user: {}, file: {} ({} bytes)", 
                   userEmail, file.getOriginalFilename(), file.getSize());
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new FileUploadResponse("File is empty"));
            }
            
            if (file.getSize() > 500 * 1024 * 1024) { // 500MB limit
                return ResponseEntity.badRequest()
                    .body(new FileUploadResponse("File size exceeds 500MB limit"));
            }
            
            // Create temporary file
            Path tempFile = Files.createTempFile("upload_", "_" + file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            
            // Upload to S3
            FileManagementService.FileMetadata metadata = fileManagementService.uploadVideoFile(
                tempFile.toFile(), file.getOriginalFilename(), userEmail);
            
            // Clean up temp file
            Files.deleteIfExists(tempFile);
            
            // Create response
            FileUploadResponse response = new FileUploadResponse(
                metadata.getFileId(),
                metadata.getOriginalFilename(),
                metadata.getS3Key(),
                metadata.getFileSizeBytes(),
                metadata.getUploadTimestamp(),
                metadata.getUserEmail(),
                metadata.getFileUrl()
            );
            
            logger.info("[File Management] File uploaded successfully: {}", response.getFileId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[File Management] File upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new FileUploadResponse("File upload failed: " + e.getMessage()));
        }
    }
    
    /**
     * List all uploaded files for a user.
     * 
     * @param userEmail The user's email
     * @return List of file metadata
     */
    @GetMapping("/list")
    public ResponseEntity<List<FileManagementService.FileMetadata>> listFiles(
            @RequestParam("userEmail") String userEmail) {
        
        logger.info("[File Management] List files request for user: {}", userEmail);
        
        try {
            List<FileManagementService.FileMetadata> files = fileManagementService.listUserFiles(userEmail);
            return ResponseEntity.ok(files);
            
        } catch (Exception e) {
            logger.error("[File Management] Failed to list files: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get file metadata by S3 key.
     * 
     * @param s3Key The S3 key of the file
     * @return File metadata
     */
    @GetMapping("/metadata")
    public ResponseEntity<FileManagementService.FileMetadata> getFileMetadata(
            @RequestParam("s3Key") String s3Key) {
        
        logger.info("[File Management] Get metadata request for file: {}", s3Key);
        
        try {
            FileManagementService.FileMetadata metadata = fileManagementService.getFileMetadata(s3Key);
            return ResponseEntity.ok(metadata);
            
        } catch (Exception e) {
            logger.error("[File Management] Failed to get file metadata: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete a file from S3.
     * 
     * @param s3Key The S3 key of the file to delete
     * @return Success response
     */
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestParam("s3Key") String s3Key) {
        
        logger.info("[File Management] Delete file request: {}", s3Key);
        
        try {
            fileManagementService.deleteFile(s3Key);
            return ResponseEntity.ok("File deleted successfully");
            
        } catch (Exception e) {
            logger.error("[File Management] Failed to delete file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to delete file: " + e.getMessage());
        }
    }
    
    /**
     * Create a translation job using an existing uploaded file.
     * 
     * @param request The translation job request
     * @return Translation job response
     */
    @PostMapping("/translate")
    public ResponseEntity<TranslationResponse> createTranslationJob(
            @RequestBody TranslationJobRequest request) {
        
        logger.info("[File Management] Translation job request for file: {}, languages: {}", 
                   request.getS3Key(), request.getTargetLanguages());
        
        try {
            // Validate that the file exists
            if (!fileManagementService.fileExists(request.getS3Key())) {
                return ResponseEntity.badRequest()
                    .body(new TranslationResponse("File not found: " + request.getS3Key()));
            }
            
            // Get file metadata
            FileManagementService.FileMetadata metadata = fileManagementService.getFileMetadata(request.getS3Key());
            
            // Create translation job
            TranslationJob job = new TranslationJob();
            job.setUserEmail(request.getUserEmail());
            job.setOriginalFilename(metadata.getOriginalFilename());
            job.setS3OriginalKey(request.getS3Key());
            job.setSourceLanguage(request.getSourceLanguage());
            job.setTargetLanguages(request.getTargetLanguages());
            
            // Save job to database first to get an ID
            job = jobRepository.save(job);
            
            // Start the translation job
            jobManager.processTranslationJob(job);
            
            // Create response
            TranslationResponse response = new TranslationResponse();
            response.setJobId(job.getId());
            response.setStatus(JobStatus.PENDING);
            response.setMessage("Translation job started successfully");
            
            logger.info("[File Management] Translation job created: {}", job.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[File Management] Failed to create translation job: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new TranslationResponse("Failed to create translation job: " + e.getMessage()));
        }
    }
    
    /**
     * Debug endpoint to list all files in S3 bucket.
     * 
     * @return List of all files in the bucket
     */
    @GetMapping("/debug/all-files")
    public ResponseEntity<List<String>> listAllFilesInBucket() {
        logger.info("[File Management] Debug request to list all files in bucket");
        
        try {
            List<String> allFiles = fileManagementService.listAllFilesInBucket();
            return ResponseEntity.ok(allFiles);
            
        } catch (Exception e) {
            logger.error("[File Management] Failed to list all files: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Health check endpoint.
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("File Management Service is running");
    }
} 