package com.videoservice.model;

/**
 * Enum representing the different states of a translation job.
 */
public enum JobStatus {
    PENDING("Pending"),
    UPLOADING("Uploading"),
    PROCESSING("Processing"),
    TRANSLATING("Translating"),
    SYNTHESIZING("Synthesizing"),
    ASSEMBLING("Assembling"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    JobStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
    
    public boolean isInProgress() {
        return this == UPLOADING || this == PROCESSING || 
               this == TRANSLATING || this == SYNTHESIZING || 
               this == ASSEMBLING;
    }
} 