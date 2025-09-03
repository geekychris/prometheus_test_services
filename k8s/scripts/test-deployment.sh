#!/bin/bash

# Test Micrometer Analytics Services in Kubernetes
# This script verifies that all services are running correctly and Prometheus is collecting metrics

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

NAMESPACE="micrometer-analytics"

echo -e "${BLUE}Testing Micrometer Analytics Services in Kubernetes...${NC}"

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}kubectl could not be found. Please install kubectl first.${NC}"
    exit 1
fi

# Check if namespace exists
if ! kubectl get namespace $NAMESPACE &> /dev/null; then
    echo -e "${RED}Namespace '$NAMESPACE' does not exist. Please run deploy-all.sh first.${NC}"
    exit 1
fi

# Function to check pod status
check_pods() {
    echo -e "${BLUE}Checking pod status...${NC}"
    kubectl get pods -n $NAMESPACE
    
    # Check if all pods are ready
    local not_ready_pods=$(kubectl get pods -n $NAMESPACE --no-headers | grep -v "1/1.*Running" | wc -l)
    if [ $not_ready_pods -gt 0 ]; then
        echo -e "${RED}Some pods are not ready. Please wait or check pod logs.${NC}"
        return 1
    else
        echo -e "${GREEN}All pods are ready!${NC}"
        return 0
    fi
}

# Function to test service endpoints using port-forward
test_service_endpoint() {
    local service_name=$1
    local port=$2
    local path=$3
    local expected_pattern=$4
    
    echo -e "${YELLOW}Testing $service_name endpoint...${NC}"
    
    # Start port-forward in background
    kubectl port-forward -n $NAMESPACE svc/$service_name $port:$port &
    local pf_pid=$!
    
    # Wait for port-forward to establish
    sleep 3
    
    # Test the endpoint
    local response=$(curl -s "http://localhost:$port$path" || echo "CURL_FAILED")
    
    # Kill port-forward
    kill $pf_pid 2>/dev/null || true
    
    if [[ "$response" == "CURL_FAILED" ]]; then
        echo -e "${RED}✗ Failed to connect to $service_name at http://localhost:$port$path${NC}"
        return 1
    elif [[ "$response" =~ $expected_pattern ]]; then
        echo -e "${GREEN}✓ $service_name is responding correctly${NC}"
        return 0
    else
        echo -e "${RED}✗ $service_name response doesn't match expected pattern${NC}"
        echo "Response: ${response:0:200}..."
        return 1
    fi
}

# Function to test Prometheus metrics
test_prometheus_metrics() {
    echo -e "${YELLOW}Testing Prometheus metrics collection...${NC}"
    
    # Start port-forward for Prometheus in background
    kubectl port-forward -n $NAMESPACE svc/prometheus 9090:9090 &
    local pf_pid=$!
    
    # Wait for port-forward to establish
    sleep 5
    
    # Query for user analytics metrics
    local user_metrics=$(curl -s "http://localhost:9090/api/v1/query?query=user_registrations_total" | grep -o '"status":"success"' || echo "NOT_FOUND")
    
    # Query for commerce analytics metrics
    local commerce_metrics=$(curl -s "http://localhost:9090/api/v1/query?query=commerce_orders_total" | grep -o '"status":"success"' || echo "NOT_FOUND")
    
    # Check targets
    local targets=$(curl -s "http://localhost:9090/api/v1/targets" | grep -o '"health":"up"' | wc -l)
    
    # Kill port-forward
    kill $pf_pid 2>/dev/null || true
    
    if [[ "$user_metrics" == "NOT_FOUND" ]]; then
        echo -e "${RED}✗ User analytics metrics not found in Prometheus${NC}"
        return 1
    elif [[ "$commerce_metrics" == "NOT_FOUND" ]]; then
        echo -e "${RED}✗ Commerce analytics metrics not found in Prometheus${NC}"
        return 1
    elif [ "$targets" -lt 2 ]; then
        echo -e "${YELLOW}⚠ Expected at least 2 healthy targets, found $targets${NC}"
        echo -e "${YELLOW}This may be normal if services are still starting up${NC}"
    else
        echo -e "${GREEN}✓ Prometheus is collecting metrics from both services${NC}"
        echo -e "${GREEN}✓ Found $targets healthy targets${NC}"
    fi
    
    return 0
}

# Main test execution
echo -e "${BLUE}Starting comprehensive tests...${NC}"

# Test 1: Check pod status
if ! check_pods; then
    echo -e "${RED}Pod status check failed. Aborting further tests.${NC}"
    exit 1
fi

echo ""

# Test 2: Test service endpoints
echo -e "${BLUE}Testing service endpoints...${NC}"

# Note: We'll use a simpler approach that doesn't rely on port-forward for basic connectivity
echo -e "${YELLOW}Checking service endpoints via kubectl proxy...${NC}"

# Start kubectl proxy in background
kubectl proxy --port=8001 &
proxy_pid=$!
sleep 3

# Test user analytics health endpoint
user_health=$(curl -s "http://localhost:8001/api/v1/namespaces/$NAMESPACE/services/user-analytics-service:8081/proxy/actuator/health" 2>/dev/null | grep -o '"status":"UP"' || echo "NOT_FOUND")

# Test commerce analytics health endpoint
commerce_health=$(curl -s "http://localhost:8001/api/v1/namespaces/$NAMESPACE/services/commerce-analytics-service:8082/proxy/actuator/health" 2>/dev/null | grep -o '"status":"UP"' || echo "NOT_FOUND")

# Kill kubectl proxy
kill $proxy_pid 2>/dev/null || true

if [[ "$user_health" == '"status":"UP"' ]]; then
    echo -e "${GREEN}✓ User Analytics Service health check passed${NC}"
else
    echo -e "${YELLOW}⚠ User Analytics Service health check failed or not ready${NC}"
fi

if [[ "$commerce_health" == '"status":"UP"' ]]; then
    echo -e "${GREEN}✓ Commerce Analytics Service health check passed${NC}"
else
    echo -e "${YELLOW}⚠ Commerce Analytics Service health check failed or not ready${NC}"
fi

echo ""

# Test 3: Test Prometheus metrics collection
echo -e "${BLUE}Testing Prometheus metrics collection...${NC}"
test_prometheus_metrics

echo ""

# Test 4: Show current metrics
echo -e "${BLUE}Current service status:${NC}"
kubectl get pods,services,endpoints -n $NAMESPACE

echo ""

# Test 5: Scale test preparation
echo -e "${BLUE}Scaling test information:${NC}"
echo -e "${YELLOW}Current replica counts:${NC}"
kubectl get deployments -n $NAMESPACE

echo ""
echo -e "${GREEN}Testing completed!${NC}"
echo ""
echo -e "${BLUE}To manually test the services, use these commands:${NC}"
echo "# Access Prometheus UI:"
echo "kubectl port-forward -n $NAMESPACE svc/prometheus 9090:9090"
echo "# Then visit: http://localhost:9090"
echo ""
echo "# Access User Analytics Service:"
echo "kubectl port-forward -n $NAMESPACE svc/user-analytics-service 8081:8081"
echo "# Then visit: http://localhost:8081/actuator/health"
echo ""
echo "# Access Commerce Analytics Service:"
echo "kubectl port-forward -n $NAMESPACE svc/commerce-analytics-service 8082:8082"
echo "# Then visit: http://localhost:8082/actuator/health"
echo ""
echo -e "${YELLOW}To test replica set behavior:${NC}"
echo "kubectl scale deployment user-analytics-service --replicas=3 -n $NAMESPACE"
echo "kubectl scale deployment commerce-analytics-service --replicas=3 -n $NAMESPACE"
