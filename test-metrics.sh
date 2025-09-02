#!/bin/bash

# Test script to verify metrics and Prometheus connectivity
set -e

echo "🧪 Testing Micrometer Services Metrics..."
echo ""

# Test User Analytics Service
echo "📊 Testing User Analytics Service (localhost:8081)..."
if curl -s http://localhost:8081/actuator/health > /dev/null; then
    echo "✅ User Analytics Service is healthy"
    USER_METRICS=$(curl -s http://localhost:8081/actuator/prometheus | grep -c "^user_")
    echo "   📈 Exposing $USER_METRICS user-specific metrics"
    
    # Show sample metrics
    echo "   🔍 Sample metrics:"
    curl -s http://localhost:8081/actuator/prometheus | grep -E "user_(active_count|registrations_total|sessions_total)" | head -3 | sed 's/^/      /'
else
    echo "❌ User Analytics Service is not responding"
fi

echo ""

# Test Commerce Analytics Service
echo "📊 Testing Commerce Analytics Service (localhost:8082)..."
if curl -s http://localhost:8082/actuator/health > /dev/null; then
    echo "✅ Commerce Analytics Service is healthy"
    COMMERCE_METRICS=$(curl -s http://localhost:8082/actuator/prometheus | grep -c "^commerce_")
    echo "   📈 Exposing $COMMERCE_METRICS commerce-specific metrics"
    
    # Show sample metrics
    echo "   🔍 Sample metrics:"
    curl -s http://localhost:8082/actuator/prometheus | grep -E "commerce_(orders_total|revenue_total|inventory_levels)" | head -3 | sed 's/^/      /'
else
    echo "❌ Commerce Analytics Service is not responding"
fi

echo ""

# Test Prometheus connectivity
echo "🎯 Testing Prometheus connectivity..."
if curl -s http://localhost:9090/api/v1/targets > /dev/null 2>&1; then
    echo "✅ Prometheus is running on localhost:9090"
    echo "   🔗 Check targets: http://localhost:9090/targets"
    echo "   🔍 Check graph: http://localhost:9090/graph"
else
    echo "❌ Prometheus is not responding on localhost:9090"
    echo ""
    echo "🐳 To start Prometheus with correct configuration:"
    echo "   docker run -d \\"
    echo "     --name prometheus \\"
    echo "     -p 9090:9090 \\"
    echo "     -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \\"
    echo "     prom/prometheus"
fi

echo ""

# Test Docker connectivity (if Prometheus is in Docker)
echo "🐳 Testing Docker to host connectivity..."
if docker ps | grep -q prometheus; then
    echo "✅ Prometheus container is running"
    echo "   Testing connectivity from container to host services..."
    
    # Test if Docker can reach host services
    if docker exec prometheus wget -qO- --timeout=5 http://host.docker.internal:8081/actuator/health 2>/dev/null | grep -q "UP"; then
        echo "   ✅ Can reach User Analytics Service from container"
    else
        echo "   ❌ Cannot reach User Analytics Service from container"
        echo "   💡 Make sure to use 'host.docker.internal:8081' in prometheus.yml"
    fi
    
    if docker exec prometheus wget -qO- --timeout=5 http://host.docker.internal:8082/actuator/health 2>/dev/null | grep -q "UP"; then
        echo "   ✅ Can reach Commerce Analytics Service from container"
    else
        echo "   ❌ Cannot reach Commerce Analytics Service from container"
        echo "   💡 Make sure to use 'host.docker.internal:8082' in prometheus.yml"
    fi
else
    echo "ℹ️  No Prometheus container currently running"
fi

echo ""
echo "🎉 Test completed!"
echo ""
echo "📋 Summary:"
echo "  • User Analytics Service: http://localhost:8081/actuator/prometheus"
echo "  • Commerce Analytics Service: http://localhost:8082/actuator/prometheus"
echo "  • Prometheus UI: http://localhost:9090"
echo "  • Prometheus Targets: http://localhost:9090/targets"
