package com.videoservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a video translation job.
 * Tracks the complete lifecycle of a translation request.
 */
@Entity
@Table(name = "translation_jobs")
public class TranslationJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @NotBlank
    @Column(name = "original_filename")
    private String originalFilename;
    
    @NotBlank
    @Column(name = "source_language")
    private String sourceLanguage;
    
    @ElementCollection
    @CollectionTable(name = "target_languages", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "language")
    private List<String> targetLanguages;
    
    @NotBlank
    @Email
    @Column(name = "user_email")
    private String userEmail;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JobStatus status;
    
    @Column(name = "progress_percentage")
    private Integer progressPercentage;
    
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    
    @Column(name = "duration_seconds")
    private Long durationSeconds;
    
    @Column(name = "s3_original_key")
    private String s3OriginalKey;
    
    @Column(name = "s3_processed_key")
    private String s3ProcessedKey;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @OneToMany(mappedBy = "translationJob", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TranslationResult> translationResults;
    
    // Constructors
    public TranslationJob() {
        this.status = JobStatus.PENDING;
        this.progressPercentage = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.translationResults = new java.util.ArrayList<>();
    }
    
    public TranslationJob(String originalFilename, String sourceLanguage, 
                         List<String> targetLanguages, String userEmail) {
        this();
        this.originalFilename = originalFilename;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguages = targetLanguages;
        this.userEmail = userEmail;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
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
        this.updatedAt = LocalDateTime.now();
    }
    
    public Integer getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
        this.updatedAt = LocalDateTime.now();
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
    
    public String getS3OriginalKey() {
        return s3OriginalKey;
    }
    
    public void setS3OriginalKey(String s3OriginalKey) {
        this.s3OriginalKey = s3OriginalKey;
    }
    
    public String getS3ProcessedKey() {
        return s3ProcessedKey;
    }
    
    public void setS3ProcessedKey(String s3ProcessedKey) {
        this.s3ProcessedKey = s3ProcessedKey;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
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
    
    public List<TranslationResult> getTranslationResults() {
        return translationResults;
    }
    
    public void setTranslationResults(List<TranslationResult> translationResults) {
        this.translationResults = translationResults;
    }
    
    // Business methods
    public void markAsCompleted() {
        this.status = JobStatus.COMPLETED;
        this.progressPercentage = 100;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateProgress(int percentage) {
        this.progressPercentage = Math.min(percentage, 100);
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "TranslationJob{" +
                "id=" + id +
                ", originalFilename='" + originalFilename + '\'' +
                ", status=" + status +
                ", progressPercentage=" + progressPercentage +
                ", userEmail='" + userEmail + '\'' +
                '}';
    }
} 