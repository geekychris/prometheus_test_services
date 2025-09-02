#!/bin/bash

# Docker Compose run script for Micrometer Services
set -e

echo "🐳 Starting Micrometer Services with Docker Compose..."

# Check if Docker and Docker Compose are available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed or not running"
    echo "Please install Docker and ensure it's running"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose is not available"
    echo "Please install Docker Compose"
    exit 1
fi

# Function to cleanup
cleanup() {
    echo ""
    echo "🛑 Stopping Docker Compose services..."
    docker-compose down
    echo "✅ Services stopped"
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Build and start services
echo "🏗️  Building and starting services..."
docker-compose up --build -d

echo ""
echo "⏳ Waiting for services to start up..."
sleep 15

# Check service health
echo "🩺 Checking service health..."

USER_HEALTH=$(curl -s http://localhost:8081/actuator/health | grep -o '"status":"UP"' || echo "DOWN")
COMMERCE_HEALTH=$(curl -s http://localhost:8082/actuator/health | grep -o '"status":"UP"' || echo "DOWN")

if [[ $USER_HEALTH == *"UP"* ]]; then
    echo "✅ User Analytics Service is healthy"
else
    echo "⚠️  User Analytics Service health check failed"
fi

if [[ $COMMERCE_HEALTH == *"UP"* ]]; then
    echo "✅ Commerce Analytics Service is healthy"
else
    echo "⚠️  Commerce Analytics Service health check failed"
fi

echo ""
echo "🎉 Docker Compose stack is running!"
echo ""
echo "📊 Service URLs:"
echo "  • User Analytics Service: http://localhost:8081"
echo "  • Commerce Analytics Service: http://localhost:8082"
echo "  • Prometheus: http://localhost:9090"
echo ""
echo "📈 Metrics endpoints:"
echo "  • User Analytics Metrics: http://localhost:8081/actuator/prometheus"
echo "  • Commerce Analytics Metrics: http://localhost:8082/actuator/prometheus"
echo ""
echo "🩺 Health checks:"
echo "  • User Analytics Health: http://localhost:8081/actuator/health"
echo "  • Commerce Analytics Health: http://localhost:8082/actuator/health"
echo ""
echo "⚡ To simulate metrics manually:"
echo "  • curl -X POST http://localhost:8081/api/simulate/all"
echo "  • curl -X POST http://localhost:8082/api/simulate/all"
echo ""
echo "📊 View logs:"
echo "  • docker-compose logs -f user-analytics"
echo "  • docker-compose logs -f commerce-analytics"
echo "  • docker-compose logs -f prometheus"
echo ""
echo "Press Ctrl+C to stop all services"

# Follow logs until interrupted
echo "📝 Following logs (Ctrl+C to stop)..."
docker-compose logs -f
