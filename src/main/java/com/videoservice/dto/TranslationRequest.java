package com.videoservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO for translation request containing video upload and translation parameters.
 */
public class TranslationRequest {
    
    @NotBlank(message = "Source language is required")
    @Size(max = 10, message = "Source language code must be at most 10 characters")
    private String sourceLanguage;
    
    @NotEmpty(message = "At least one target language must be specified")
    @Size(max = 10, message = "Maximum 10 target languages allowed")
    private List<String> targetLanguages;
    
    @NotBlank(message = "User email is required")
    @Email(message = "Invalid email format")
    private String userEmail;
    
    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;
    
    // Constructors
    public TranslationRequest() {}
    
    public TranslationRequest(String sourceLanguage, List<String> targetLanguages, String userEmail) {
        this.sourceLanguage = sourceLanguage;
        this.targetLanguages = targetLanguages;
        this.userEmail = userEmail;
    }
    
    public TranslationRequest(String sourceLanguage, List<String> targetLanguages, 
                            String userEmail, String description) {
        this(sourceLanguage, targetLanguages, userEmail);
        this.description = description;
    }
    
    // Getters and Setters
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "TranslationRequest{" +
                "sourceLanguage='" + sourceLanguage + '\'' +
                ", targetLanguages=" + targetLanguages +
                ", userEmail='" + userEmail + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
} 