package com.videoservice.dto;

import com.videoservice.model.TranslationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for individual translation result response.
 */
public class TranslationResultResponse {
    
    private UUID id;
    private String targetLanguage;
    private String s3VideoKey;
    private String downloadUrl;
    private Double translationQualityScore;
    private Double audioQualityScore;
    private Long processingTimeSeconds;
    private Long fileSizeBytes;
    private String errorMessage;
    private TranslationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    
    // Constructors
    public TranslationResultResponse() {}
    
    public TranslationResultResponse(UUID id, String targetLanguage, TranslationStatus status) {
        this.id = id;
        this.targetLanguage = targetLanguage;
        this.status = status;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getTargetLanguage() {
        return targetLanguage;
    }
    
    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }
    
    public String getS3VideoKey() {
        return s3VideoKey;
    }
    
    public void setS3VideoKey(String s3VideoKey) {
        this.s3VideoKey = s3VideoKey;
    }
    
    public String getDownloadUrl() {
        return downloadUrl;
    }
    
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
    
    public Double getTranslationQualityScore() {
        return translationQualityScore;
    }
    
    public void setTranslationQualityScore(Double translationQualityScore) {
        this.translationQualityScore = translationQualityScore;
    }
    
    public Double getAudioQualityScore() {
        return audioQualityScore;
    }
    
    public void setAudioQualityScore(Double audioQualityScore) {
        this.audioQualityScore = audioQualityScore;
    }
    
    public Long getProcessingTimeSeconds() {
        return processingTimeSeconds;
    }
    
    public void setProcessingTimeSeconds(Long processingTimeSeconds) {
        this.processingTimeSeconds = processingTimeSeconds;
    }
    
    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }
    
    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public TranslationStatus getStatus() {
        return status;
    }
    
    public void setStatus(TranslationStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    @Override
    public String toString() {
        return "TranslationResultResponse{" +
                "id=" + id +
                ", targetLanguage='" + targetLanguage + '\'' +
                ", status=" + status +
                ", s3VideoKey='" + s3VideoKey + '\'' +
                '}';
    }
} 