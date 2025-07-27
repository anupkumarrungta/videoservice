package com.videoservice.dto;

import java.util.List;

/**
 * DTO for translation job requests using existing uploaded files.
 */
public class TranslationJobRequest {
    private String userEmail;
    private String s3Key; // S3 key of the uploaded file
    private String sourceLanguage;
    private List<String> targetLanguages;
    
    // Default constructor
    public TranslationJobRequest() {}
    
    // Constructor with parameters
    public TranslationJobRequest(String userEmail, String s3Key, String sourceLanguage, List<String> targetLanguages) {
        this.userEmail = userEmail;
        this.s3Key = s3Key;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguages = targetLanguages;
    }
    
    // Getters and setters
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getS3Key() {
        return s3Key;
    }
    
    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
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
} 