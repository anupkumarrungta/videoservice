package com.videoservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.transcribe.TranscribeClient;
import software.amazon.awssdk.services.translate.TranslateClient;

/**
 * Configuration class for AWS services.
 * Sets up AWS clients with proper credentials and region settings.
 */
@Configuration
public class AwsConfig {
    
    @Value("${aws.region:us-east-1}")
    private String region;
    
    @Value("${aws.credentials.access-key}")
    private String accessKey;
    
    @Value("${aws.credentials.secret-key}")
    private String secretKey;
    
    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;
    
    @Value("${aws.s3.force-path-style:false}")
    private boolean forcePathStyle;
    
    /**
     * Configure S3 client.
     * 
     * @return S3Client instance
     */
    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
        
        // Configure endpoint for local development or custom S3-compatible storage
        if (s3Endpoint != null && !s3Endpoint.isEmpty()) {
            builder.endpointOverride(java.net.URI.create(s3Endpoint));
        }
        
        // Configure path style for local development
        if (forcePathStyle) {
            builder.forcePathStyle(true);
        }
        
        return builder.build();
    }
    
    /**
     * Configure S3 presigner for generating presigned URLs.
     * 
     * @return S3Presigner instance
     */
    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
        
        // Configure endpoint for local development
        if (s3Endpoint != null && !s3Endpoint.isEmpty()) {
            builder.endpointOverride(java.net.URI.create(s3Endpoint));
        }
        
        return builder.build();
    }
    
    /**
     * Configure Translate client.
     * 
     * @return TranslateClient instance
     */
    @Bean
    public TranslateClient translateClient() {
        return TranslateClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
    
    /**
     * Configure Polly client for text-to-speech.
     * 
     * @return PollyClient instance
     */
    @Bean
    public PollyClient pollyClient() {
        return PollyClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
    
    /**
     * Configure Transcribe client for speech-to-text.
     * 
     * @return TranscribeClient instance
     */
    @Bean
    public TranscribeClient transcribeClient() {
        return TranscribeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
} 