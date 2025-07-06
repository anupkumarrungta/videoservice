#!/bin/bash

# Video Translation Service Setup Script
# This script helps set up the development environment

set -e

echo "🎬 Video Translation Service Setup"
echo "=================================="

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    echo "   Visit: https://docs.docker.com/get-docker/"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    echo "   Visit: https://docs.docker.com/compose/install/"
    exit 1
fi

echo "✅ Docker and Docker Compose are installed"

# Create necessary directories
echo "📁 Creating directories..."
mkdir -p logs temp

# Set up environment variables
echo "🔧 Setting up environment variables..."

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    cat > .env << EOF
# AWS Configuration (for local development with MinIO)
AWS_ACCESS_KEY_ID=minioadmin
AWS_SECRET_ACCESS_KEY=minioadmin
AWS_REGION=us-east-1
S3_BUCKET_NAME=video-translation-bucket
S3_ENDPOINT=http://localhost:9000
AWS_S3_FORCE_PATH_STYLE=true

# Email Configuration (optional for development)
MAIL_USERNAME=test@example.com
MAIL_PASSWORD=testpassword

# Application Configuration
SPRING_PROFILES_ACTIVE=dev
JAVA_OPTS=-Xmx2g -Xms1g

# FFmpeg paths
FFMPEG_PATH=/usr/bin/ffmpeg
FFPROBE_PATH=/usr/bin/ffprobe
EOF
    echo "✅ Created .env file"
else
    echo "ℹ️  .env file already exists"
fi

# Build the application
echo "🔨 Building the application..."
docker-compose build

# Start the services
echo "🚀 Starting services..."
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 30

# Check if services are running
echo "🔍 Checking service status..."

# Check MinIO
if curl -f http://localhost:9000/minio/health/live &> /dev/null; then
    echo "✅ MinIO is running"
else
    echo "❌ MinIO is not responding"
fi

# Check Video Translation Service
if curl -f http://localhost:8080/api/v1/translation/health &> /dev/null; then
    echo "✅ Video Translation Service is running"
else
    echo "❌ Video Translation Service is not responding"
fi

echo ""
echo "🎉 Setup completed!"
echo ""
echo "📋 Service URLs:"
echo "   • Video Translation API: http://localhost:8080"
echo "   • MinIO Console: http://localhost:9001 (admin/minioadmin)"
echo "   • H2 Database Console: http://localhost:8080/h2-console"
echo ""
echo "📚 Next steps:"
echo "   1. Access the MinIO console to create the S3 bucket"
echo "   2. Test the API using the provided endpoints"
echo "   3. Check the logs: docker-compose logs -f"
echo ""
echo "🛠️  Useful commands:"
echo "   • Stop services: docker-compose down"
echo "   • View logs: docker-compose logs -f"
echo "   • Restart services: docker-compose restart"
echo "   • Clean up: docker-compose down -v"
echo ""
echo "📖 For more information, see the README.md file" 