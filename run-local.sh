#!/bin/bash

# Local run script for Micrometer Services
set -e

echo "🚀 Starting Micrometer Services locally..."

# Check if services are already built
if [ ! -f "user-analytics-service/target/user-analytics-service-0.0.1-SNAPSHOT.jar" ] || [ ! -f "commerce-analytics-service/target/commerce-analytics-service-0.0.1-SNAPSHOT.jar" ]; then
    echo "📦 Services not built yet. Building now..."
    ./build.sh
fi

# Function to cleanup background processes
cleanup() {
    echo ""
    echo "🛑 Stopping services..."
    if [ ! -z "$USER_PID" ]; then
        kill $USER_PID 2>/dev/null || true
    fi
    if [ ! -z "$COMMERCE_PID" ]; then
        kill $COMMERCE_PID 2>/dev/null || true
    fi
    echo "✅ Services stopped"
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

echo ""
echo "🔄 Starting services..."

# Start User Analytics Service
echo "Starting User Analytics Service on port 8081..."
cd user-analytics-service
mvn spring-boot:run &
USER_PID=$!
cd ..

# Wait a moment for the first service to start
sleep 3

# Start Commerce Analytics Service
echo "Starting Commerce Analytics Service on port 8082..."
cd commerce-analytics-service
mvn spring-boot:run &
COMMERCE_PID=$!
cd ..

echo ""
echo "🎉 Both services are starting up!"
echo ""
echo "📊 Service URLs:"
echo "  • User Analytics Service: http://localhost:8081"
echo "  • Commerce Analytics Service: http://localhost:8082"
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
echo "Press Ctrl+C to stop all services"
echo ""

# Wait for services to start
echo "Waiting for services to start..."
sleep 10

# Check if services are running
if curl -s http://localhost:8081/actuator/health > /dev/null; then
    echo "✅ User Analytics Service is healthy"
else
    echo "⚠️  User Analytics Service may still be starting..."
fi

if curl -s http://localhost:8082/actuator/health > /dev/null; then
    echo "✅ Commerce Analytics Service is healthy"
else
    echo "⚠️  Commerce Analytics Service may still be starting..."
fi

echo ""
echo "🎯 Both services are now running. Monitoring for shutdown signal..."

# Wait for termination
wait
