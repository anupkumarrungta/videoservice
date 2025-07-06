# videoservice
This is video translation service which allows translating audio from source input to other languages on behalf of content producers.


# AI Agent Prompt: Multilingual Content Translation Platform

## Project Overview
You are tasked with implementing a comprehensive multilingual content translation platform in Java that enables content creators (independent journalists, teachers, educators) to translate their video content audio into multiple languages while preserving video quality, emotions, and context.

## Core Requirements

### 1. Application Purpose
- Enable content producers to reach global audiences by translating their video audio content
- Support independent journalists, teachers, and educators who create content on platforms like YouTube, Instagram
- Maintain video quality while replacing audio with translated versions
- Preserve emotional tone, context, and meaning during translation

### 2. Supported Languages
Implement translation support for:
- English
- Arabic
- South Korean
- Chinese (Mandarin)
- Tamil
- Hindi (source language example)

### 3. Technical Architecture Requirements

#### Core Components to Implement:

1. **Video Processing Service**
   - Extract audio from video files
   - Maintain video stream integrity
   - Support common video formats (MP4, AVI, MOV, etc.)

2. **Audio Chunking Service**
   - Split audio into ~3-minute chunks
   - Respect sentence boundaries (no mid-sentence cuts)
   - Maintain audio quality and continuity

3. **Translation Service**
   - Preserve emotional tone and context
   - Maintain meaning accuracy
   - Handle cultural nuances appropriately

4. **Audio Synthesis Service**
   - Generate natural-sounding speech in target languages
   - Match original speaker's pace and emotional delivery
   - Maintain lip-sync compatibility

5. **Video Assembly Service**
   - Merge translated audio with original video
   - Ensure audio-video synchronization
   - Preserve video quality and visual elements

6. **Storage Management**
   - AWS S3 integration for file storage
   - Implement naming convention: `filename_lang_[language]`
   - Example: `news_report_lang_english.mp4`, `news_report_lang_arabic.mp4`

7. **Notification Service**
   - Email notifications for job completion
   - Progress tracking and status updates
   - Error handling and user communication

8. **User Interface**
   - Simple, intuitive UX for content creators
   - File upload functionality
   - Language selection interface
   - Progress monitoring dashboard

### 4. Implementation Specifications

#### Java Framework and Libraries:
- **Spring Boot** for application framework
- **Spring Security** for authentication
- **Spring Cloud AWS** for S3 integration
- **Apache Commons IO** for file operations
- **JavaMail API** for email notifications
- **Jackson** for JSON processing
- **Maven** for dependency management

#### Required Dependencies:
```xml
<!-- Add these to your pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-aws</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

#### Key Classes to Implement:

1. **VideoTranslationController**
   - Handle file uploads
   - Manage translation requests
   - Return job status and results

2. **VideoProcessingService**
   - Extract audio from video
   - Manage video-audio separation

3. **AudioChunkingService**
   - Split audio into manageable chunks
   - Preserve sentence boundaries

4. **TranslationService**
   - Interface with translation APIs
   - Maintain context and emotion

5. **AudioSynthesisService**
   - Generate speech in target languages
   - Match original audio characteristics

6. **S3StorageService**
   - Handle file uploads/downloads
   - Implement naming conventions
   - Manage storage lifecycle

7. **NotificationService**
   - Send completion emails
   - Handle error notifications

8. **JobManager**
   - Manage asynchronous processing
   - Track job status and progress

### 5. Technical Constraints and Requirements

#### Performance Requirements:
- Process videos up to 60 minutes in length
- Support concurrent translation jobs
- Maintain 99% uptime for the service
- Optimize for cost-effective S3 storage

#### Quality Requirements:
- Preserve original video quality
- Maintain audio-video synchronization
- Ensure natural-sounding translations
- Preserve emotional tone and context

#### Security Requirements:
- Secure file upload and storage
- User authentication and authorization
- Data encryption in transit and at rest
- Compliance with content creator privacy needs

### 6. Integration Requirements

#### AWS Services:
- **S3** for file storage with lifecycle policies
- **SQS** for job queue management
- **SNS** for notifications
- **CloudWatch** for monitoring and logging

#### External APIs:
- Translation service APIs (Google Translate, AWS Translate, or similar)
- Text-to-speech services (AWS Polly, Google TTS, or similar)
- Video processing libraries (FFmpeg integration)

### 7. File Processing Workflow

1. **Upload Phase**
   - User uploads video file
   - System validates file format and size
   - Generate unique job ID

2. **Processing Phase**
   - Extract audio from video
   - Chunk audio into ~3-minute segments
   - Translate each chunk while preserving context
   - Generate speech in target languages
   - Reassemble video with new audio

3. **Storage Phase**
   - Upload processed files to S3
   - Apply naming convention
   - Set appropriate metadata and permissions

4. **Notification Phase**
   - Send completion email with S3 links
   - Provide preview functionality
   - Include download instructions

### 8. Error Handling and Resilience

- Implement comprehensive error handling
- Provide retry mechanisms for failed jobs
- Maintain detailed logging for debugging
- Implement circuit breaker patterns for external service calls
- Handle partial failures gracefully

### 9. Configuration Management

Create application properties for:
- AWS credentials and region settings
- S3 bucket configurations
- Email service settings
- Translation service API keys
- Processing parameters (chunk size, quality settings)

### 10. Testing Requirements

Implement comprehensive testing:
- Unit tests for core services
- Integration tests for AWS services
- End-to-end testing for complete workflows
- Performance testing for large files
- Security testing for file uploads

## Implementation Priority

1. **Phase 1**: Basic video upload and S3 storage
2. **Phase 2**: Audio extraction and chunking
3. **Phase 3**: Translation service integration
4. **Phase 4**: Audio synthesis and video assembly
5. **Phase 5**: Notification system and UX polish
6. **Phase 6**: Performance optimization and scaling

## Success Metrics

- Translation accuracy and emotional preservation
- Processing time per video minute
- User satisfaction with output quality
- System reliability and uptime
- Cost efficiency of operations

## Deliverables Expected

1. Complete Java application with all services
2. Docker configuration for deployment
3. AWS infrastructure setup scripts
4. User documentation and API documentation
5. Testing suite with coverage reports
6. Performance benchmarks and optimization recommendations

Begin implementation by setting up the core Spring Boot application structure and AWS S3 integration, then progressively add the video processing, translation, and notification capabilities.
