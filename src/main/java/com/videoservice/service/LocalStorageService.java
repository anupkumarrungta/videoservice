package com.videoservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Local storage service for handling file operations when S3 is not available.
 * This provides a fallback for testing and development environments.
 */
@Service
public class LocalStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalStorageService.class);
    
    private final Path storageBasePath;
    
    public LocalStorageService(@Value("${storage.local.base-path:./uploads}") String basePath) {
        this.storageBasePath = Paths.get(basePath);
        createStorageDirectoryIfNotExists();
    }
    
    /**
     * Upload a file to local storage with a unique key.
     * 
     * @param file The file to upload
     * @param originalFilename The original filename for naming convention
     * @return The storage key of the uploaded file
     */
    public String uploadFile(File file, String originalFilename) throws IOException {
        String storageKey = generateStorageKey(originalFilename);
        Path targetPath = storageBasePath.resolve(storageKey);
        
        try {
            // Create parent directories if they don't exist
            Files.createDirectories(targetPath.getParent());
            
            // Copy the file to the target location
            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            logger.info("Successfully uploaded file {} to local storage with key: {}", originalFilename, storageKey);
            return storageKey;
            
        } catch (IOException e) {
            logger.error("Failed to upload file {} to local storage: {}", originalFilename, e.getMessage());
            throw new IOException("Failed to upload file to local storage", e);
        }
    }
    
    /**
     * Upload a file to local storage with a specific key.
     * 
     * @param file The file to upload
     * @param storageKey The specific storage key to use
     * @return The storage key of the uploaded file
     */
    public String uploadFileWithKey(File file, String storageKey) throws IOException {
        Path targetPath = storageBasePath.resolve(storageKey);
        
        try {
            // Create parent directories if they don't exist
            Files.createDirectories(targetPath.getParent());
            
            // Copy the file to the target location
            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            logger.info("Successfully uploaded file to local storage with key: {}", storageKey);
            return storageKey;
            
        } catch (IOException e) {
            logger.error("Failed to upload file to local storage with key {}: {}", storageKey, e.getMessage());
            throw new IOException("Failed to upload file to local storage", e);
        }
    }
    
    /**
     * Get a file from local storage.
     * 
     * @param storageKey The storage key of the file
     * @return The file
     */
    public File getFile(String storageKey) {
        Path filePath = storageBasePath.resolve(storageKey);
        File file = filePath.toFile();
        
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + storageKey);
        }
        
        return file;
    }
    
    /**
     * Get a file path from local storage.
     * 
     * @param storageKey The storage key of the file
     * @return The file path
     */
    public Path getFilePath(String storageKey) {
        return storageBasePath.resolve(storageKey);
    }
    
    /**
     * Delete a file from local storage.
     * 
     * @param storageKey The storage key of the file to delete
     */
    public void deleteFile(String storageKey) {
        try {
            Path filePath = storageBasePath.resolve(storageKey);
            Files.deleteIfExists(filePath);
            
            logger.info("Successfully deleted file from local storage with key: {}", storageKey);
            
        } catch (IOException e) {
            logger.error("Failed to delete file from local storage key {}: {}", storageKey, e.getMessage());
            throw new RuntimeException("Failed to delete file from local storage", e);
        }
    }
    
    /**
     * Check if a file exists in local storage.
     * 
     * @param storageKey The storage key to check
     * @return true if the file exists, false otherwise
     */
    public boolean fileExists(String storageKey) {
        Path filePath = storageBasePath.resolve(storageKey);
        return Files.exists(filePath);
    }
    
    /**
     * Get the file size in bytes.
     * 
     * @param storageKey The storage key of the file
     * @return The file size in bytes
     */
    public long getFileSize(String storageKey) {
        try {
            Path filePath = storageBasePath.resolve(storageKey);
            return Files.size(filePath);
        } catch (IOException e) {
            logger.error("Failed to get file size for key {}: {}", storageKey, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Generate a unique storage key for a file.
     * 
     * @param originalFilename The original filename
     * @return A unique storage key
     */
    private String generateStorageKey(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString();
        String extension = getFileExtension(originalFilename);
        
        return String.format("uploads/%s/%s%s", timestamp, uuid, extension);
    }
    
    /**
     * Generate a storage key for translated video.
     * 
     * @param originalFilename The original filename
     * @param language The target language
     * @return A storage key for the translated video
     */
    public String generateTranslatedVideoKey(String originalFilename, String language) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString();
        String baseName = getBaseName(originalFilename);
        
        return String.format("translated/%s/%s_%s_%s.mp4", timestamp, baseName, language, uuid);
    }
    
    /**
     * Create the storage directory if it doesn't exist.
     */
    private void createStorageDirectoryIfNotExists() {
        try {
            if (!Files.exists(storageBasePath)) {
                Files.createDirectories(storageBasePath);
                logger.info("Created local storage directory: {}", storageBasePath.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to create storage directory: {}", e.getMessage());
            throw new RuntimeException("Failed to create storage directory", e);
        }
    }
    
    /**
     * Get the file extension from a filename.
     * 
     * @param filename The filename
     * @return The file extension (including the dot)
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }
    
    /**
     * Get the base name (without extension) from a filename.
     * 
     * @param filename The filename
     * @return The base name without extension
     */
    private String getBaseName(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(0, lastDotIndex) : filename;
    }
} 