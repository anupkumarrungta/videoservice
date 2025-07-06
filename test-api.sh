#!/bin/bash

# Video Translation Service API Test Script
# This script tests the main API endpoints

set -e

BASE_URL="http://localhost:8080/api/v1/translation"

echo "🧪 Testing Video Translation Service API"
echo "========================================"

# Function to make HTTP requests and display results
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo "Testing: $description"
    echo "Endpoint: $method $BASE_URL$endpoint"
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$BASE_URL$endpoint")
    elif [ "$method" = "POST" ]; then
        if [ -n "$data" ]; then
            response=$(curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" -d "$data" "$BASE_URL$endpoint")
        else
            response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$endpoint")
        fi
    fi
    
    # Extract status code (last line)
    status_code=$(echo "$response" | tail -n1)
    # Extract response body (all lines except last)
    body=$(echo "$response" | head -n -1)
    
    echo "Status: $status_code"
    echo "Response: $body"
    echo "---"
}

# Test health endpoint
test_endpoint "GET" "/health" "" "Health Check"

# Test supported languages
test_endpoint "GET" "/languages" "" "Get Supported Languages"

# Test system statistics
test_endpoint "GET" "/stats" "" "Get System Statistics"

# Test getting jobs (should return empty or error without authentication)
test_endpoint "GET" "/jobs?userEmail=test@example.com&page=0&size=10" "" "Get User Jobs"

echo "✅ API tests completed!"
echo ""
echo "📝 Note: Some endpoints may require authentication or specific data."
echo "   For full testing, you'll need to:"
echo "   1. Upload a video file using the /upload endpoint"
echo "   2. Use the returned job ID to test other endpoints"
echo ""
echo "📖 See the README.md for complete API documentation" 