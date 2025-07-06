package com.videoservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Video Translation Service.
 * This Spring Boot application provides multilingual content translation
 * capabilities for video content creators.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class VideoTranslationApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoTranslationApplication.class, args);
    }
} 