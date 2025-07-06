package com.videoservice.repository;

import com.videoservice.model.JobStatus;
import com.videoservice.model.TranslationJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for TranslationJob entities.
 * Provides database operations for translation jobs.
 */
@Repository
public interface TranslationJobRepository extends JpaRepository<TranslationJob, UUID> {
    
    /**
     * Find jobs by user email.
     * 
     * @param userEmail The user email
     * @param pageable Pagination parameters
     * @return Page of translation jobs
     */
    Page<TranslationJob> findByUserEmailOrderByCreatedAtDesc(String userEmail, Pageable pageable);
    
    /**
     * Find jobs by status.
     * 
     * @param status The job status
     * @param pageable Pagination parameters
     * @return Page of translation jobs
     */
    Page<TranslationJob> findByStatusOrderByCreatedAtDesc(JobStatus status, Pageable pageable);
    
    /**
     * Find jobs by user email and status.
     * 
     * @param userEmail The user email
     * @param status The job status
     * @param pageable Pagination parameters
     * @return Page of translation jobs
     */
    Page<TranslationJob> findByUserEmailAndStatusOrderByCreatedAtDesc(
            String userEmail, JobStatus status, Pageable pageable);
    
    /**
     * Find jobs created after a specific date.
     * 
     * @param date The date to filter from
     * @param pageable Pagination parameters
     * @return Page of translation jobs
     */
    Page<TranslationJob> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime date, Pageable pageable);
    
    /**
     * Find jobs by source language.
     * 
     * @param sourceLanguage The source language
     * @param pageable Pagination parameters
     * @return Page of translation jobs
     */
    Page<TranslationJob> findBySourceLanguageOrderByCreatedAtDesc(String sourceLanguage, Pageable pageable);
    
    /**
     * Find jobs that contain a specific target language.
     * 
     * @param targetLanguage The target language to search for
     * @param pageable Pagination parameters
     * @return Page of translation jobs
     */
    @Query("SELECT tj FROM TranslationJob tj WHERE :targetLanguage MEMBER OF tj.targetLanguages ORDER BY tj.createdAt DESC")
    Page<TranslationJob> findByTargetLanguageContaining(@Param("targetLanguage") String targetLanguage, Pageable pageable);
    
    /**
     * Find jobs by original filename (case-insensitive).
     * 
     * @param filename The filename to search for
     * @param pageable Pagination parameters
     * @return Page of translation jobs
     */
    Page<TranslationJob> findByOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(String filename, Pageable pageable);
    
    /**
     * Count jobs by status.
     * 
     * @param status The job status
     * @return Number of jobs with the given status
     */
    long countByStatus(JobStatus status);
    
    /**
     * Count jobs by user email.
     * 
     * @param userEmail The user email
     * @return Number of jobs for the user
     */
    long countByUserEmail(String userEmail);
    
    /**
     * Find jobs that are currently in progress.
     * 
     * @return List of jobs that are being processed
     */
    @Query("SELECT tj FROM TranslationJob tj WHERE tj.status IN ('UPLOADING', 'PROCESSING', 'TRANSLATING', 'SYNTHESIZING', 'ASSEMBLING') ORDER BY tj.createdAt ASC")
    List<TranslationJob> findJobsInProgress();
    
    /**
     * Find jobs that have been completed recently.
     * 
     * @param hours Number of hours to look back
     * @return List of recently completed jobs
     */
    @Query("SELECT tj FROM TranslationJob tj WHERE tj.status = 'COMPLETED' AND tj.completedAt >= :hoursAgo ORDER BY tj.completedAt DESC")
    List<TranslationJob> findRecentlyCompletedJobs(@Param("hoursAgo") LocalDateTime hoursAgo);
    
    /**
     * Find jobs that have failed recently.
     * 
     * @param hours Number of hours to look back
     * @return List of recently failed jobs
     */
    @Query("SELECT tj FROM TranslationJob tj WHERE tj.status = 'FAILED' AND tj.updatedAt >= :hoursAgo ORDER BY tj.updatedAt DESC")
    List<TranslationJob> findRecentlyFailedJobs(@Param("hoursAgo") LocalDateTime hoursAgo);
    
    /**
     * Find jobs by file size range.
     * 
     * @param minSize Minimum file size in bytes
     * @param maxSize Maximum file size in bytes
     * @param pageable Pagination parameters
     * @return Page of translation jobs
     */
    Page<TranslationJob> findByFileSizeBytesBetweenOrderByCreatedAtDesc(
            Long minSize, Long maxSize, Pageable pageable);
    
    /**
     * Find jobs by duration range.
     * 
     * @param minDuration Minimum duration in seconds
     * @param maxDuration Maximum duration in seconds
     * @param pageable Pagination parameters
     * @return Page of translation jobs
     */
    Page<TranslationJob> findByDurationSecondsBetweenOrderByCreatedAtDesc(
            Long minDuration, Long maxDuration, Pageable pageable);
    
    /**
     * Delete jobs older than a specific date.
     * 
     * @param date The date to delete jobs before
     * @return Number of jobs deleted
     */
    @Query("DELETE FROM TranslationJob tj WHERE tj.createdAt < :date")
    int deleteJobsOlderThan(@Param("date") LocalDateTime date);
    
    /**
     * Find jobs with error messages containing specific text.
     * 
     * @param errorText The error text to search for
     * @param pageable Pagination parameters
     * @return Page of translation jobs
     */
    Page<TranslationJob> findByErrorMessageContainingIgnoreCaseOrderByCreatedAtDesc(String errorText, Pageable pageable);
    
    /**
     * Get statistics about job statuses.
     * 
     * @return List of status counts
     */
    @Query("SELECT tj.status, COUNT(tj) FROM TranslationJob tj GROUP BY tj.status")
    List<Object[]> getJobStatusStatistics();
    
    /**
     * Get statistics about jobs by language.
     * 
     * @return List of language counts
     */
    @Query("SELECT tj.sourceLanguage, COUNT(tj) FROM TranslationJob tj GROUP BY tj.sourceLanguage")
    List<Object[]> getSourceLanguageStatistics();
} 