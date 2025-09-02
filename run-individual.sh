#!/bin/bash

# Script to run individual services for testing
set -e

SERVICE=$1

if [ -z "$SERVICE" ]; then
    echo "Usage: $0 <service>"
    echo ""
    echo "Available services:"
    echo "  • user-analytics"
    echo "  • commerce-analytics"
    echo ""
    echo "Example: $0 user-analytics"
    exit 1
fi

case $SERVICE in
    "user-analytics")
        echo "🚀 Starting User Analytics Service..."
        if [ ! -f "user-analytics-service/target/user-analytics-service-0.0.1-SNAPSHOT.jar" ]; then
            echo "📦 Building User Analytics Service..."
            mvn -f user-analytics-service/pom.xml clean package -DskipTests
        fi
        
        echo "🔄 Running User Analytics Service on port 8081..."
        echo "📊 Service URL: http://localhost:8081"
        echo "📈 Metrics: http://localhost:8081/actuator/prometheus"
        echo "🩺 Health: http://localhost:8081/actuator/health"
        echo ""
        
        cd user-analytics-service
        mvn spring-boot:run
        ;;
        
    "commerce-analytics")
        echo "🚀 Starting Commerce Analytics Service..."
        if [ ! -f "commerce-analytics-service/target/commerce-analytics-service-0.0.1-SNAPSHOT.jar" ]; then
            echo "📦 Building Commerce Analytics Service..."
            mvn -f commerce-analytics-service/pom.xml clean package -DskipTests
        fi
        
        echo "🔄 Running Commerce Analytics Service on port 8082..."
        echo "📊 Service URL: http://localhost:8082"
        echo "📈 Metrics: http://localhost:8082/actuator/prometheus"
        echo "🩺 Health: http://localhost:8082/actuator/health"
        echo ""
        
        cd commerce-analytics-service
        mvn spring-boot:run
        ;;
        
    *)
        echo "❌ Unknown service: $SERVICE"
        echo ""
        echo "Available services:"
        echo "  • user-analytics"
        echo "  • commerce-analytics"
        exit 1
        ;;
esac
