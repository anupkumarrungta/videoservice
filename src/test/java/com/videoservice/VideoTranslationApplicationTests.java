package com.videoservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic test to verify that the Spring Boot application context loads correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
class VideoTranslationApplicationTests {

    @Test
    void contextLoads() {
        // This test will pass if the application context loads successfully
    }
} 