#!/bin/bash

# Build script for Micrometer Services
set -e

echo "🏗️  Building Micrometer Services..."

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed or not in PATH"
    echo "Please install Maven to build the project"
    exit 1
fi

# Check Java version
echo "☕ Using Java version:"
java -version

# Clean and build all modules
echo ""
echo "📦 Building all services..."
mvn clean package -DskipTests

echo ""
echo "✅ Build completed successfully!"
echo ""
echo "📊 Services built:"
echo "  • User Analytics Service: user-analytics-service/target/user-analytics-service-0.0.1-SNAPSHOT.jar"
echo "  • Commerce Analytics Service: commerce-analytics-service/target/commerce-analytics-service-0.0.1-SNAPSHOT.jar"
echo ""
echo "🐳 To build Docker images, run: ./docker-build.sh"
echo "🚀 To run services locally, run: ./run-local.sh"
echo "🔧 To run with Docker Compose, run: ./run-docker.sh"
