package com.videoservice.service;

import com.videoservice.model.JobStatus;
import com.videoservice.model.TranslationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service for handling email notifications for translation jobs.
 * Sends completion emails, progress updates, and error notifications.
 */
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final S3StorageService s3StorageService;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${spring.application.name:Video Translation Service}")
    private String applicationName;
    
    @Value("${notification.email.subject.prefix:[Video Translation]}")
    private String emailSubjectPrefix;
    
    public NotificationService(JavaMailSender mailSender, 
                             TemplateEngine templateEngine,
                             S3StorageService s3StorageService) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.s3StorageService = s3StorageService;
    }
    
    /**
     * Send job completion notification.
     * 
     * @param job The completed translation job
     * @param downloadUrls Map of language to download URL
     */
    public void sendJobCompletionNotification(TranslationJob job, Map<String, String> downloadUrls) {
        logger.info("Sending job completion notification for job: {} to: {}", job.getId(), job.getUserEmail());
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(job.getUserEmail());
            helper.setSubject(emailSubjectPrefix + " Translation Completed - " + job.getOriginalFilename());
            
            // Create context for template
            Context context = new Context();
            context.setVariable("job", job);
            context.setVariable("downloadUrls", downloadUrls);
            context.setVariable("applicationName", applicationName);
            context.setVariable("completionTime", job.getCompletedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // Process template
            String htmlContent = templateEngine.process("job-completion", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Job completion notification sent successfully to: {}", job.getUserEmail());
            
        } catch (MessagingException e) {
            logger.error("Failed to send job completion notification: {}", e.getMessage());
        }
    }
    
    /**
     * Send job failure notification.
     * 
     * @param job The failed translation job
     */
    public void sendJobFailureNotification(TranslationJob job) {
        logger.info("Sending job failure notification for job: {} to: {}", job.getId(), job.getUserEmail());
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(job.getUserEmail());
            helper.setSubject(emailSubjectPrefix + " Translation Failed - " + job.getOriginalFilename());
            
            // Create context for template
            Context context = new Context();
            context.setVariable("job", job);
            context.setVariable("applicationName", applicationName);
            context.setVariable("errorMessage", job.getErrorMessage());
            context.setVariable("failureTime", job.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // Process template
            String htmlContent = templateEngine.process("job-failure", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Job failure notification sent successfully to: {}", job.getUserEmail());
            
        } catch (MessagingException e) {
            logger.error("Failed to send job failure notification: {}", e.getMessage());
        }
    }
    
    /**
     * Send job progress update notification.
     * 
     * @param job The translation job with updated progress
     */
    public void sendProgressUpdateNotification(TranslationJob job) {
        logger.debug("Sending progress update notification for job: {} to: {}", job.getId(), job.getUserEmail());
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(job.getUserEmail());
            helper.setSubject(emailSubjectPrefix + " Progress Update - " + job.getOriginalFilename());
            
            // Create context for template
            Context context = new Context();
            context.setVariable("job", job);
            context.setVariable("applicationName", applicationName);
            context.setVariable("progressPercentage", job.getProgressPercentage());
            context.setVariable("status", job.getStatus().getDisplayName());
            context.setVariable("updateTime", job.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // Process template
            String htmlContent = templateEngine.process("progress-update", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.debug("Progress update notification sent successfully to: {}", job.getUserEmail());
            
        } catch (MessagingException e) {
            logger.error("Failed to send progress update notification: {}", e.getMessage());
        }
    }
    
    /**
     * Send job started notification.
     * 
     * @param job The translation job that has started
     */
    public void sendJobStartedNotification(TranslationJob job) {
        logger.info("Sending job started notification for job: {} to: {}", job.getId(), job.getUserEmail());
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(job.getUserEmail());
            helper.setSubject(emailSubjectPrefix + " Translation Started - " + job.getOriginalFilename());
            
            // Create context for template
            Context context = new Context();
            context.setVariable("job", job);
            context.setVariable("applicationName", applicationName);
            context.setVariable("startTime", job.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // Process template
            String htmlContent = templateEngine.process("job-started", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Job started notification sent successfully to: {}", job.getUserEmail());
            
        } catch (MessagingException e) {
            logger.error("Failed to send job started notification: {}", e.getMessage());
        }
    }
    
    /**
     * Send simple text email notification.
     * 
     * @param toEmail The recipient email
     * @param subject The email subject
     * @param message The email message
     */
    public void sendSimpleNotification(String toEmail, String subject, String message) {
        logger.info("Sending simple notification to: {}", toEmail);
        
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(toEmail);
            mailMessage.setSubject(emailSubjectPrefix + " " + subject);
            mailMessage.setText(message);
            
            mailSender.send(mailMessage);
            logger.info("Simple notification sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            logger.error("Failed to send simple notification: {}", e.getMessage());
        }
    }
    
    /**
     * Send system error notification to administrators.
     * 
     * @param errorMessage The error message
     * @param stackTrace The stack trace
     * @param adminEmails List of admin email addresses
     */
    public void sendSystemErrorNotification(String errorMessage, String stackTrace, List<String> adminEmails) {
        logger.error("Sending system error notification to administrators");
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(adminEmails.toArray(new String[0]));
            helper.setSubject(emailSubjectPrefix + " System Error Alert");
            
            // Create context for template
            Context context = new Context();
            context.setVariable("errorMessage", errorMessage);
            context.setVariable("stackTrace", stackTrace);
            context.setVariable("applicationName", applicationName);
            context.setVariable("errorTime", java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // Process template
            String htmlContent = templateEngine.process("system-error", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("System error notification sent successfully to administrators");
            
        } catch (MessagingException e) {
            logger.error("Failed to send system error notification: {}", e.getMessage());
        }
    }
    
    /**
     * Send welcome notification to new users.
     * 
     * @param userEmail The user's email address
     * @param userName The user's name
     */
    public void sendWelcomeNotification(String userEmail, String userName) {
        logger.info("Sending welcome notification to: {}", userEmail);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject(emailSubjectPrefix + " Welcome to " + applicationName);
            
            // Create context for template
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("applicationName", applicationName);
            context.setVariable("welcomeTime", java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // Process template
            String htmlContent = templateEngine.process("welcome", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Welcome notification sent successfully to: {}", userEmail);
            
        } catch (MessagingException e) {
            logger.error("Failed to send welcome notification: {}", e.getMessage());
        }
    }
    
    /**
     * Generate download URLs for completed translations.
     * 
     * @param job The translation job
     * @return Map of language to download URL
     */
    public Map<String, String> generateDownloadUrls(TranslationJob job) {
        Map<String, String> downloadUrls = new java.util.HashMap<>();
        
        if (job.getTranslationResults() != null) {
            for (var result : job.getTranslationResults()) {
                if (result.getS3VideoKey() != null && result.getStatus().isTerminal()) {
                    try {
                        String downloadUrl = s3StorageService.getPresignedDownloadUrl(result.getS3VideoKey(), 24 * 60); // 24 hours
                        downloadUrls.put(result.getTargetLanguage(), downloadUrl);
                    } catch (Exception e) {
                        logger.error("Failed to generate download URL for language {}: {}", result.getTargetLanguage(), e.getMessage());
                    }
                }
            }
        }
        
        return downloadUrls;
    }
    
    /**
     * Check if email notifications are enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isEmailNotificationsEnabled() {
        return mailSender != null && fromEmail != null && !fromEmail.isEmpty();
    }
} 