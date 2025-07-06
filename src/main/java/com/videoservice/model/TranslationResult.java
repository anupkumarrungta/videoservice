package com.videoservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing the result of a translation for a specific target language.
 */
@Entity
@Table(name = "translation_results")
public class TranslationResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private TranslationJob translationJob;
    
    @NotBlank
    @Column(name = "target_language")
    private String targetLanguage;
    
    @Column(name = "s3_audio_key")
    private String s3AudioKey;
    
    @Column(name = "s3_video_key")
    private String s3VideoKey;
    
    @Column(name = "translation_quality_score")
    private Double translationQualityScore;
    
    @Column(name = "audio_quality_score")
    private Double audioQualityScore;
    
    @Column(name = "processing_time_seconds")
    private Long processingTimeSeconds;
    
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TranslationStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    // Constructors
    public TranslationResult() {
        this.status = TranslationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }
    
    public TranslationResult(TranslationJob translationJob, String targetLanguage) {
        this();
        this.translationJob = translationJob;
        this.targetLanguage = targetLanguage;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public TranslationJob getTranslationJob() {
        return translationJob;
    }
    
    public void setTranslationJob(TranslationJob translationJob) {
        this.translationJob = translationJob;
    }
    
    public String getTargetLanguage() {
        return targetLanguage;
    }
    
    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }
    
    public String getS3AudioKey() {
        return s3AudioKey;
    }
    
    public void setS3AudioKey(String s3AudioKey) {
        this.s3AudioKey = s3AudioKey;
    }
    
    public String getS3VideoKey() {
        return s3VideoKey;
    }
    
    public void setS3VideoKey(String s3VideoKey) {
        this.s3VideoKey = s3VideoKey;
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
    
    // Business methods
    public void markAsCompleted() {
        this.status = TranslationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String errorMessage) {
        this.status = TranslationStatus.FAILED;
        this.errorMessage = errorMessage;
    }
    
    public String getS3KeyForLanguage() {
        if (translationJob != null && translationJob.getOriginalFilename() != null) {
            String baseName = translationJob.getOriginalFilename().replaceFirst("[.][^.]+$", "");
            return baseName + "_lang_" + targetLanguage.toLowerCase() + ".mp4";
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "TranslationResult{" +
                "id=" + id +
                ", targetLanguage='" + targetLanguage + '\'' +
                ", status=" + status +
                ", s3VideoKey='" + s3VideoKey + '\'' +
                '}';
    }
} 