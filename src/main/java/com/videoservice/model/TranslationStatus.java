package com.videoservice.model;

/**
 * Enum representing the different states of a translation result.
 */
public enum TranslationStatus {
    PENDING("Pending"),
    PROCESSING("Processing"),
    TRANSLATING("Translating"),
    SYNTHESIZING("Synthesizing"),
    ASSEMBLING("Assembling"),
    COMPLETED("Completed"),
    FAILED("Failed");
    
    private final String displayName;
    
    TranslationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
    
    public boolean isInProgress() {
        return this == PROCESSING || this == TRANSLATING || 
               this == SYNTHESIZING || this == ASSEMBLING;
    }
} 