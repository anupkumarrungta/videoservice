package com.videoservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing uploaded video files in S3.
 * Provides file upload, listing, and management capabilities.
 */
@Service
public class FileManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileManagementService.class);
    
    private final S3Client s3Client;
    private final S3StorageService s3StorageService;
    
    @Value("${aws.s3.bucket-name:video-translation-bucket2}")
    private String bucketName;
    
    @Value("${aws.region:us-east-1}")
    private String awsRegion;
    
    public FileManagementService(S3Client s3Client, S3StorageService s3StorageService) {
        this.s3Client = s3Client;
        this.s3StorageService = s3StorageService;
    }
    
    /**
     * Upload a video file to S3 and return file metadata.
     * 
     * @param file The video file to upload
     * @param originalFilename The original filename
     * @param userEmail The user's email for organization
     * @return File metadata including S3 key and URL
     * @throws Exception if upload fails
     */
    public FileMetadata uploadVideoFile(java.io.File file, String originalFilename, String userEmail) throws Exception {
        logger.info("[File Management] Uploading video file: {} for user: {}", originalFilename, userEmail);
        
        // Generate unique S3 key with user organization
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String sanitizedEmail = userEmail.replaceAll("[^a-zA-Z0-9]", "_");
        String s3Key = String.format("uploads/%s/%s_%s", sanitizedEmail, timestamp, originalFilename);
        
        // Upload file to S3
        s3StorageService.uploadFileWithKey(file, s3Key);
        
        // Get file metadata
        FileMetadata metadata = new FileMetadata();
        metadata.setOriginalFilename(originalFilename);
        metadata.setS3Key(s3Key);
        metadata.setFileSizeBytes(file.length());
        metadata.setUploadTimestamp(LocalDateTime.now());
        metadata.setUserEmail(userEmail);
        metadata.setFileUrl(s3StorageService.getFileUrl(s3Key));
        metadata.setFileId(generateFileId());
        
        logger.info("[File Management] File uploaded successfully: {}", metadata);
        return metadata;
    }
    
    /**
     * List all uploaded files for a user.
     * 
     * @param userEmail The user's email
     * @return List of file metadata
     * @throws Exception if listing fails
     */
    public List<FileMetadata> listUserFiles(String userEmail) throws Exception {
        logger.info("[File Management] Listing files for user: {}", userEmail);
        
        List<FileMetadata> files = new ArrayList<>();
        
        // First, try the new organized structure
        String sanitizedEmail = userEmail.replaceAll("[^a-zA-Z0-9]", "_");
        String newPrefix = "uploads/" + sanitizedEmail + "/";
        
        ListObjectsRequest newRequest = ListObjectsRequest.builder()
                .bucket(bucketName)
                .prefix(newPrefix)
                .build();
        
        ListObjectsResponse newResponse = s3Client.listObjects(newRequest);
        
        if (newResponse.contents() != null) {
            for (S3Object s3Object : newResponse.contents()) {
                if (!s3Object.key().endsWith("/")) {
                    FileMetadata metadata = createFileMetadata(s3Object, userEmail);
                    files.add(metadata);
                }
            }
        }
        
        // If no files found in new structure, try to find files in legacy structure
        if (files.isEmpty()) {
            logger.info("[File Management] No files found in new structure, searching legacy structure for user: {}", userEmail);
            
            // List all objects in the bucket and filter by user email
            ListObjectsRequest allRequest = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            ListObjectsResponse allResponse = s3Client.listObjects(allRequest);
            
            if (allResponse.contents() != null) {
                for (S3Object s3Object : allResponse.contents()) {
                    if (!s3Object.key().endsWith("/")) {
                        // Check if this file belongs to the user by looking at the key
                        String key = s3Object.key();
                        
                        // Skip transcription results and other non-user files
                        if (key.contains("transcription-results/") || 
                            key.contains("translated-videos/") ||
                            key.contains("scripts/")) {
                            continue;
                        }
                        
                        // For legacy files, assume they belong to the current user
                        // You can add more sophisticated logic here if needed
                        FileMetadata metadata = createFileMetadata(s3Object, userEmail);
                        files.add(metadata);
                    }
                }
            }
        }
        
        // Sort by upload date (newest first)
        files.sort((a, b) -> b.getLastModified().compareTo(a.getLastModified()));
        
        logger.info("[File Management] Found {} files for user: {}", files.size(), userEmail);
        return files;
    }
    
    /**
     * Create file metadata from S3 object.
     * 
     * @param s3Object The S3 object
     * @param userEmail The user's email
     * @return File metadata
     */
    private FileMetadata createFileMetadata(S3Object s3Object, String userEmail) {
        String originalFilename = extractOriginalFilename(s3Object.key());
        
        FileMetadata metadata = new FileMetadata();
        metadata.setS3Key(s3Object.key());
        metadata.setOriginalFilename(originalFilename);
        metadata.setFileSizeBytes(s3Object.size());
        metadata.setLastModified(Date.from(s3Object.lastModified()));
        metadata.setUserEmail(userEmail);
        metadata.setFileUrl(s3StorageService.getFileUrl(s3Object.key()));
        metadata.setFileId(generateFileIdFromKey(s3Object.key()));
        
        return metadata;
    }
    
    /**
     * Get file metadata by S3 key.
     * 
     * @param s3Key The S3 key of the file
     * @return File metadata
     * @throws Exception if retrieval fails
     */
    public FileMetadata getFileMetadata(String s3Key) throws Exception {
        logger.info("[File Management] Getting metadata for file: {}", s3Key);
        
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        
        HeadObjectResponse response = s3Client.headObject(request);
        
        FileMetadata metadata = new FileMetadata();
        metadata.setS3Key(s3Key);
        metadata.setOriginalFilename(extractOriginalFilename(s3Key));
        metadata.setFileSizeBytes(response.contentLength());
        metadata.setLastModified(Date.from(response.lastModified()));
        metadata.setFileUrl(s3StorageService.getFileUrl(s3Key));
        metadata.setFileId(generateFileIdFromKey(s3Key));
        
        // Extract user email from S3 key
        String userEmail = extractUserEmailFromKey(s3Key);
        metadata.setUserEmail(userEmail);
        
        logger.info("[File Management] Retrieved metadata: {}", metadata);
        return metadata;
    }
    
    /**
     * Delete a file from S3.
     * 
     * @param s3Key The S3 key of the file to delete
     * @throws Exception if deletion fails
     */
    public void deleteFile(String s3Key) throws Exception {
        logger.info("[File Management] Deleting file: {}", s3Key);
        
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        
        s3Client.deleteObject(request);
        logger.info("[File Management] File deleted successfully: {}", s3Key);
    }
    
    /**
     * List all files in the S3 bucket (for debugging).
     * 
     * @return List of all file keys in the bucket
     * @throws Exception if listing fails
     */
    public List<String> listAllFilesInBucket() throws Exception {
        logger.info("[File Management] Listing all files in bucket: {}", bucketName);
        
        ListObjectsRequest request = ListObjectsRequest.builder()
                .bucket(bucketName)
                .build();
        
        ListObjectsResponse response = s3Client.listObjects(request);
        
        List<String> allFiles = new ArrayList<>();
        
        if (response.contents() != null) {
            for (S3Object s3Object : response.contents()) {
                if (!s3Object.key().endsWith("/")) {
                    allFiles.add(s3Object.key());
                }
            }
        }
        
        logger.info("[File Management] Found {} total files in bucket", allFiles.size());
        return allFiles;
    }
    
    /**
     * Check if a file exists in S3.
     * 
     * @param s3Key The S3 key to check
     * @return true if file exists, false otherwise
     */
    public boolean fileExists(String s3Key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            s3Client.headObject(request);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Generate a unique file ID.
     */
    private String generateFileId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Generate a file ID from S3 key.
     */
    private String generateFileIdFromKey(String s3Key) {
        // Use the timestamp part of the S3 key as file ID
        String[] parts = s3Key.split("/");
        if (parts.length >= 3) {
            return parts[2].split("_")[0]; // First part of filename (timestamp)
        }
        return generateFileId();
    }
    
    /**
     * Extract original filename from S3 key.
     */
    private String extractOriginalFilename(String s3Key) {
        String[] parts = s3Key.split("/");
        if (parts.length >= 3) {
            String filename = parts[2];
            // Remove timestamp prefix
            String[] filenameParts = filename.split("_", 2);
            if (filenameParts.length >= 2) {
                return filenameParts[1];
            }
        }
        return s3Key;
    }
    
    /**
     * Extract user email from S3 key.
     */
    private String extractUserEmailFromKey(String s3Key) {
        String[] parts = s3Key.split("/");
        if (parts.length >= 2) {
            return parts[1].replaceAll("_", "@");
        }
        return "unknown";
    }
    
    /**
     * File metadata class.
     */
    public static class FileMetadata {
        private String fileId;
        private String originalFilename;
        private String s3Key;
        private long fileSizeBytes;
        private LocalDateTime uploadTimestamp;
        private Date lastModified;
        private String userEmail;
        private String fileUrl;
        
        // Getters and setters
        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }
        
        public String getOriginalFilename() { return originalFilename; }
        public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
        
        public String getS3Key() { return s3Key; }
        public void setS3Key(String s3Key) { this.s3Key = s3Key; }
        
        public long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
        
        public LocalDateTime getUploadTimestamp() { return uploadTimestamp; }
        public void setUploadTimestamp(LocalDateTime uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }
        
        public Date getLastModified() { return lastModified; }
        public void setLastModified(Date lastModified) { this.lastModified = lastModified; }
        
        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
        
        public String getFileUrl() { return fileUrl; }
        public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
        
        @Override
        public String toString() {
            return String.format("FileMetadata{fileId='%s', filename='%s', size=%d bytes, user='%s'}", 
                               fileId, originalFilename, fileSizeBytes, userEmail);
        }
    }
} 