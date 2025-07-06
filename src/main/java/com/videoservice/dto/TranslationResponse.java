package com.videoservice.dto;

import com.videoservice.model.JobStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for translation response containing job status and results.
 */
public class TranslationResponse {
    
    private UUID jobId;
    private String originalFilename;
    private String sourceLanguage;
    private List<String> targetLanguages;
    private String userEmail;
    private JobStatus status;
    private Integer progressPercentage;
    private Long fileSizeBytes;
    private Long durationSeconds;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private List<TranslationResultResponse> results;
    
    // Constructors
    public TranslationResponse() {}
    
    public TranslationResponse(UUID jobId, String originalFilename, JobStatus status) {
        this.jobId = jobId;
        this.originalFilename = originalFilename;
        this.status = status;
    }
    
    // Getters and Setters
    public UUID getJobId() {
        return jobId;
    }
    
    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }
    
    public String getOriginalFilename() {
        return originalFilename;
    }
    
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
    
    public String getSourceLanguage() {
        return sourceLanguage;
    }
    
    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }
    
    public List<String> getTargetLanguages() {
        return targetLanguages;
    }
    
    public void setTargetLanguages(List<String> targetLanguages) {
        this.targetLanguages = targetLanguages;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public JobStatus getStatus() {
        return status;
    }
    
    public void setStatus(JobStatus status) {
        this.status = status;
    }
    
    public Integer getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }
    
    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }
    
    public Long getDurationSeconds() {
        return durationSeconds;
    }
    
    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public List<TranslationResultResponse> getResults() {
        return results;
    }
    
    public void setResults(List<TranslationResultResponse> results) {
        this.results = results;
    }
    
    @Override
    public String toString() {
        return "TranslationResponse{" +
                "jobId=" + jobId +
                ", originalFilename='" + originalFilename + '\'' +
                ", status=" + status +
                ", progressPercentage=" + progressPercentage +
                ", userEmail='" + userEmail + '\'' +
                '}';
    }
} 