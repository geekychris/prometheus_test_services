#!/bin/bash

# Docker build script for Micrometer Services
set -e

echo "🐳 Building Docker images for Micrometer Services..."

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed or not running"
    echo "Please install Docker and ensure it's running"
    exit 1
fi

# Build Maven artifacts first
echo "📦 Building Maven artifacts..."
./build.sh

echo ""
echo "🏗️  Building Docker images..."

# Build User Analytics Service Docker image
echo "Building User Analytics Service Docker image..."
cd user-analytics-service
docker build -t user-analytics-service:latest .
echo "✅ User Analytics Service image built successfully"
cd ..

# Build Commerce Analytics Service Docker image
echo "Building Commerce Analytics Service Docker image..."
cd commerce-analytics-service
docker build -t commerce-analytics-service:latest .
echo "✅ Commerce Analytics Service image built successfully"
cd ..

echo ""
echo "🎉 All Docker images built successfully!"
echo ""
echo "📊 Available images:"
docker images | grep -E "(user-analytics-service|commerce-analytics-service)" | head -2

echo ""
echo "🚀 To run with Docker Compose: ./run-docker.sh"
echo "🔍 To run individual containers:"
echo "  • User Analytics: docker run -p 8081:8081 user-analytics-service:latest"
echo "  • Commerce Analytics: docker run -p 8082:8082 commerce-analytics-service:latest"
