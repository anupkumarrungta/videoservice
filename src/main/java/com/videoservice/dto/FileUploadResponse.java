package com.videoservice.dto;

import java.time.LocalDateTime;

/**
 * DTO for file upload responses.
 */
public class FileUploadResponse {
    private String fileId;
    private String originalFilename;
    private String s3Key;
    private long fileSizeBytes;
    private LocalDateTime uploadTimestamp;
    private String userEmail;
    private String fileUrl;
    private String message;
    private boolean success;
    
    // Default constructor
    public FileUploadResponse() {}
    
    // Success constructor
    public FileUploadResponse(String fileId, String originalFilename, String s3Key, 
                            long fileSizeBytes, LocalDateTime uploadTimestamp, 
                            String userEmail, String fileUrl) {
        this.fileId = fileId;
        this.originalFilename = originalFilename;
        this.s3Key = s3Key;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadTimestamp = uploadTimestamp;
        this.userEmail = userEmail;
        this.fileUrl = fileUrl;
        this.success = true;
        this.message = "File uploaded successfully";
    }
    
    // Error constructor
    public FileUploadResponse(String message) {
        this.success = false;
        this.message = message;
    }
    
    // Getters and setters
    public String getFileId() {
        return fileId;
    }
    
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    public String getOriginalFilename() {
        return originalFilename;
    }
    
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
    
    public String getS3Key() {
        return s3Key;
    }
    
    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }
    
    public long getFileSizeBytes() {
        return fileSizeBytes;
    }
    
    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }
    
    public LocalDateTime getUploadTimestamp() {
        return uploadTimestamp;
    }
    
    public void setUploadTimestamp(LocalDateTime uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
} 