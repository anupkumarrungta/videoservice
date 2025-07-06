# Video Translation Service

A comprehensive multilingual content translation platform that enables content creators (independent journalists, teachers, educators) to translate their video content audio into multiple languages while preserving video quality, emotions, and context.

## Features

- **Multi-language Support**: English, Arabic, Korean, Chinese, Tamil, Hindi
- **Video Processing**: Extract audio, chunk processing, maintain video quality
- **AI Translation**: AWS Translate integration with context preservation
- **Text-to-Speech**: AWS Polly integration for natural-sounding speech
- **Cloud Storage**: AWS S3 integration with proper naming conventions
- **Email Notifications**: Job completion and progress updates
- **RESTful API**: Complete API for job management
- **Asynchronous Processing**: Non-blocking job processing
- **Progress Tracking**: Real-time job status and progress monitoring

## Architecture

The application follows a microservices architecture with the following components:

- **VideoProcessingService**: Handles video file operations using FFmpeg
- **TranslationService**: Manages text translation using AWS Translate
- **AudioSynthesisService**: Generates speech using AWS Polly
- **S3StorageService**: Manages file storage and retrieval
- **NotificationService**: Handles email notifications
- **JobManager**: Coordinates the entire translation workflow

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- FFmpeg (for video processing)
- AWS Account with access to:
  - S3 (for file storage)
  - Translate (for text translation)
  - Polly (for text-to-speech)
- Docker and Docker Compose (for containerized deployment)

## Quick Start

### Option 1: Docker Compose (Recommended for Development)

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd videoservice
   ```

2. **Start the services**:
   ```bash
   docker-compose up -d
   ```

3. **Access the services**:
   - Video Translation API: http://localhost:8080
   - MinIO Console: http://localhost:9001 (admin/minioadmin)
   - H2 Database Console: http://localhost:8080/h2-console

### Option 2: Local Development

1. **Install FFmpeg**:
   ```bash
   # Ubuntu/Debian
   sudo apt-get install ffmpeg
   
   # macOS
   brew install ffmpeg
   
   # Windows
   # Download from https://ffmpeg.org/download.html
   ```

2. **Configure AWS credentials**:
   ```bash
   export AWS_ACCESS_KEY_ID=your-access-key
   export AWS_SECRET_ACCESS_KEY=your-secret-key
   export AWS_REGION=us-east-1
   ```

3. **Build and run**:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `AWS_ACCESS_KEY_ID` | AWS access key | Required |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key | Required |
| `AWS_REGION` | AWS region | us-east-1 |
| `S3_BUCKET_NAME` | S3 bucket name | video-translation-bucket |
| `MAIL_USERNAME` | Email username | Required for notifications |
| `MAIL_PASSWORD` | Email password | Required for notifications |

### Application Properties

Key configuration options in `application.yml`:

```yaml
# Video processing
video:
  max-duration: 3600  # 60 minutes
  supported-formats: [mp4, avi, mov, mkv, wmv]

# Audio processing
audio:
  chunk-duration: 180  # 3 minutes
  sample-rate: 16000
  bit-rate: 128000

# Job processing
job:
  max-concurrent-jobs: 5
  retry-attempts: 3
  timeout: 3600000  # 1 hour
```

## API Documentation

### Upload Video for Translation

```http
POST /api/v1/translation/upload
Content-Type: multipart/form-data

file: [video file]
sourceLanguage: english
targetLanguages: [arabic, korean, chinese]
userEmail: user@example.com
description: Optional description
```

**Response**:
```json
{
  "jobId": "uuid",
  "originalFilename": "video.mp4",
  "status": "PENDING",
  "progressPercentage": 0,
  "userEmail": "user@example.com",
  "createdAt": "2024-01-01T10:00:00"
}
```

### Get Job Status

```http
GET /api/v1/translation/job/{jobId}
```

**Response**:
```json
{
  "jobId": "uuid",
  "originalFilename": "video.mp4",
  "status": "COMPLETED",
  "progressPercentage": 100,
  "results": [
    {
      "targetLanguage": "arabic",
      "status": "COMPLETED",
      "downloadUrl": "https://...",
      "fileSizeBytes": 1234567
    }
  ]
}
```

### Get User Jobs

```http
GET /api/v1/translation/jobs?userEmail=user@example.com&page=0&size=20
```

### Cancel Job

```http
POST /api/v1/translation/job/{jobId}/cancel
```

### Retry Failed Job

```http
POST /api/v1/translation/job/{jobId}/retry
```

### Get Download URL

```http
GET /api/v1/translation/job/{jobId}/download/{language}
```

### Get System Statistics

```http
GET /api/v1/translation/stats
```

### Get Supported Languages

```http
GET /api/v1/translation/languages
```

## Job Status Flow

1. **PENDING**: Job created, waiting to be processed
2. **UPLOADING**: File being uploaded to S3
3. **PROCESSING**: Video being processed (audio extraction, chunking)
4. **TRANSLATING**: Text being translated
5. **SYNTHESIZING**: Speech being synthesized
6. **ASSEMBLING**: Final video being assembled
7. **COMPLETED**: Job completed successfully
8. **FAILED**: Job failed with error
9. **CANCELLED**: Job cancelled by user

## File Naming Convention

Translated videos follow the naming convention:
```
{original_filename}_lang_{language}.mp4
```

Examples:
- `news_report_lang_english.mp4`
- `news_report_lang_arabic.mp4`
- `news_report_lang_korean.mp4`

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/videoservice/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/            # Data transfer objects
│   │   ├── model/          # Entity classes
│   │   ├── repository/     # Data access layer
│   │   ├── service/        # Business logic services
│   │   └── VideoTranslationApplication.java
│   └── resources/
│       ├── application.yml # Main configuration
│       └── templates/      # Email templates
└── test/
    └── java/com/videoservice/
        └── VideoTranslationApplicationTests.java
```

### Running Tests

```bash
mvn test
```

### Code Coverage

```bash
mvn jacoco:report
```

## Deployment

### Production Deployment

1. **Set up AWS infrastructure**:
   - Create S3 bucket
   - Configure IAM roles and policies
   - Set up CloudWatch for monitoring

2. **Configure environment variables**:
   ```bash
   export AWS_ACCESS_KEY_ID=production-access-key
   export AWS_SECRET_ACCESS_KEY=production-secret-key
   export S3_BUCKET_NAME=production-bucket
   export MAIL_USERNAME=production-email
   export MAIL_PASSWORD=production-password
   ```

3. **Build and deploy**:
   ```bash
   mvn clean package
   java -jar target/video-translation-service-1.0.0.jar
   ```

### Docker Deployment

```bash
docker build -t video-translation-service .
docker run -p 8080:8080 \
  -e AWS_ACCESS_KEY_ID=your-key \
  -e AWS_SECRET_ACCESS_KEY=your-secret \
  video-translation-service
```

## Monitoring and Health Checks

### Health Endpoint

```http
GET /api/v1/translation/health
```

### Actuator Endpoints

- `/actuator/health` - Application health
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

## Troubleshooting

### Common Issues

1. **FFmpeg not found**: Ensure FFmpeg is installed and in PATH
2. **AWS credentials**: Verify AWS credentials are properly configured
3. **S3 bucket**: Ensure S3 bucket exists and is accessible
4. **Email configuration**: Check SMTP settings for notifications

### Logs

Application logs are available at:
- Console output
- `./logs/` directory (when using Docker)

### Performance Tuning

- Adjust `job.max-concurrent-jobs` based on server capacity
- Configure JVM memory settings (`-Xmx`, `-Xms`)
- Use appropriate S3 storage class for cost optimization

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation

## Roadmap

- [ ] Speech-to-text integration (AWS Transcribe)
- [ ] Advanced audio processing (noise reduction, audio enhancement)
- [ ] Web interface for job management
- [ ] Batch processing capabilities
- [ ] Advanced translation quality metrics
- [ ] Multi-tenant support
- [ ] API rate limiting and quotas
- [ ] Advanced security features
