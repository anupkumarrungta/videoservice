package com.videoservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

/**
 * Service for handling AWS S3 storage operations including file uploads,
 * downloads, and URL generation with proper naming conventions.
 */
@Service
public class S3StorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String s3Endpoint;
    
    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner,
                           @Value("${aws.s3.bucket-name}") String bucketName,
                           @Value("${aws.s3.endpoint:}") String s3Endpoint) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.s3Endpoint = s3Endpoint;
    }
    
    /**
     * Upload a file to S3 with a unique key.
     * 
     * @param file The file to upload
     * @param originalFilename The original filename for naming convention
     * @return The S3 key of the uploaded file
     */
    public String uploadFile(File file, String originalFilename) throws IOException {
        String s3Key = generateS3Key(originalFilename);
        
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(getContentType(originalFilename))
                    .metadata(createMetadata(file, originalFilename))
                    .build();
            
            RequestBody requestBody = RequestBody.fromFile(file);
            
            PutObjectResponse response = s3Client.putObject(putObjectRequest, requestBody);
            
            logger.info("Successfully uploaded file {} to S3 with key: {}", originalFilename, s3Key);
            return s3Key;
            
        } catch (S3Exception e) {
            logger.error("Failed to upload file {} to S3: {}", originalFilename, e.getMessage());
            throw new IOException("Failed to upload file to S3", e);
        }
    }
    
    /**
     * Upload a file to S3 with a specific key.
     * 
     * @param file The file to upload
     * @param s3Key The specific S3 key to use
     * @return The S3 key of the uploaded file
     */
    public String uploadFileWithKey(File file, String s3Key) throws IOException {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(getContentType(s3Key))
                    .build();
            
            RequestBody requestBody = RequestBody.fromFile(file);
            
            PutObjectResponse response = s3Client.putObject(putObjectRequest, requestBody);
            
            logger.info("Successfully uploaded file to S3 with key: {}", s3Key);
            return s3Key;
            
        } catch (S3Exception e) {
            logger.error("Failed to upload file to S3 with key {}: {}", s3Key, e.getMessage());
            throw new IOException("Failed to upload file to S3", e);
        }
    }
    
    /**
     * Download a file from S3 to a local path.
     * 
     * @param s3Key The S3 key of the file to download
     * @param localPath The local path where to save the file
     * @return The downloaded file
     */
    public File downloadFile(String s3Key, Path localPath) throws IOException {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            // Delete existing file if it exists to avoid conflicts
            if (Files.exists(localPath)) {
                Files.delete(localPath);
                logger.debug("Deleted existing file at: {}", localPath);
            }
            
            File localFile = localPath.toFile();
            s3Client.getObject(getObjectRequest, localPath);
            
            logger.info("Successfully downloaded file from S3 key: {} to: {}", s3Key, localPath);
            return localFile;
            
        } catch (S3Exception e) {
            logger.error("Failed to download file from S3 key {}: {}", s3Key, e.getMessage());
            throw new IOException("Failed to download file from S3", e);
        } catch (IOException e) {
            logger.error("Failed to handle local file for S3 download {}: {}", s3Key, e.getMessage());
            throw new IOException("Failed to handle local file for S3 download", e);
        }
    }
    
    /**
     * Get a presigned URL for downloading a file.
     * 
     * @param s3Key The S3 key of the file
     * @param expirationMinutes The expiration time in minutes
     * @return The presigned URL
     */
    public String getPresignedDownloadUrl(String s3Key, int expirationMinutes) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .build())
                    .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();
            
            logger.info("Generated presigned URL for S3 key: {}", s3Key);
            return presignedUrl;
            
        } catch (Exception e) {
            logger.error("Failed to generate presigned URL for S3 key {}: {}", s3Key, e.getMessage());
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }
    
    /**
     * Get a simple file URL for logging purposes.
     * 
     * @param s3Key The S3 key of the file
     * @return The file URL
     */
    public String getFileUrl(String s3Key) {
        if (s3Endpoint != null && !s3Endpoint.isEmpty()) {
            // Local development with MinIO
            return s3Endpoint + "/" + bucketName + "/" + s3Key;
        } else {
            // AWS S3
            return "https://" + bucketName + ".s3.amazonaws.com/" + s3Key;
        }
    }
    
    /**
     * Delete a file from S3.
     * 
     * @param s3Key The S3 key of the file to delete
     */
    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            DeleteObjectResponse response = s3Client.deleteObject(deleteObjectRequest);
            
            logger.info("Successfully deleted file from S3 with key: {}", s3Key);
            
        } catch (S3Exception e) {
            logger.error("Failed to delete file from S3 key {}: {}", s3Key, e.getMessage());
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }
    
    /**
     * Check if a file exists in S3.
     * 
     * @param s3Key The S3 key to check
     * @return true if the file exists, false otherwise
     */
    public boolean fileExists(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            s3Client.headObject(headObjectRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            logger.error("Error checking if file exists in S3 key {}: {}", s3Key, e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate a unique S3 key for a file.
     * 
     * @param originalFilename The original filename
     * @return A unique S3 key
     */
    private String generateS3Key(String originalFilename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString();
        String extension = getFileExtension(originalFilename);
        
        return String.format("uploads/%s/%s%s", timestamp, uuid, extension);
    }
    
    /**
     * Generate S3 key for translated video with language suffix.
     * 
     * @param originalFilename The original filename
     * @param language The target language
     * @return The S3 key for the translated video
     */
    public String generateTranslatedVideoKey(String originalFilename, String language) {
        String baseName = originalFilename.replaceFirst("[.][^.]+$", "");
        return String.format("translated/%s_lang_%s.mp4", baseName, language.toLowerCase());
    }
    
    /**
     * Get the file extension from a filename.
     * 
     * @param filename The filename
     * @return The file extension including the dot
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }
    
    /**
     * Get the content type based on file extension.
     * 
     * @param filename The filename
     * @return The content type
     */
    private String getContentType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        
        return switch (extension) {
            case ".mp4" -> "video/mp4";
            case ".avi" -> "video/x-msvideo";
            case ".mov" -> "video/quicktime";
            case ".mkv" -> "video/x-matroska";
            case ".wmv" -> "video/x-ms-wmv";
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            case ".m4a" -> "audio/mp4";
            default -> "application/octet-stream";
        };
    }
    
    /**
     * Create metadata for the S3 object.
     * 
     * @param file The file
     * @param originalFilename The original filename
     * @return The metadata map
     */
    private java.util.Map<String, String> createMetadata(File file, String originalFilename) {
        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("original-filename", originalFilename);
        metadata.put("file-size", String.valueOf(file.length()));
        metadata.put("upload-timestamp", String.valueOf(System.currentTimeMillis()));
        return metadata;
    }
    
    /**
     * Get the file size of an S3 object.
     * 
     * @param s3Key The S3 key
     * @return The file size in bytes
     */
    public long getFileSize(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            return response.contentLength();
            
        } catch (S3Exception e) {
            logger.error("Failed to get file size for S3 key {}: {}", s3Key, e.getMessage());
            throw new RuntimeException("Failed to get file size", e);
        }
    }
} 