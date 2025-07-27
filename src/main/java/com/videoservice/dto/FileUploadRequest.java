package com.videoservice.dto;

/**
 * DTO for file upload requests.
 */
public class FileUploadRequest {
    private String userEmail;
    private String originalFilename;
    private long fileSizeBytes;
    
    // Default constructor
    public FileUploadRequest() {}
    
    // Constructor with parameters
    public FileUploadRequest(String userEmail, String originalFilename, long fileSizeBytes) {
        this.userEmail = userEmail;
        this.originalFilename = originalFilename;
        this.fileSizeBytes = fileSizeBytes;
    }
    
    // Getters and setters
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getOriginalFilename() {
        return originalFilename;
    }
    
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
    
    public long getFileSizeBytes() {
        return fileSizeBytes;
    }
    
    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }
} 