#!/bin/bash

# Troubleshooting Script for Micrometer Analytics Kubernetes Deployment
# This script helps diagnose common deployment issues

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

NAMESPACE="micrometer-analytics"

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE} Micrometer Analytics Troubleshooting${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# Function to print section headers
print_section() {
    echo ""
    echo -e "${BLUE}=== $1 ===${NC}"
    echo ""
}

# Function to check command availability
check_command() {
    local cmd=$1
    local desc=$2
    
    if command -v "$cmd" &> /dev/null; then
        echo -e "${GREEN}✓ $desc available${NC} ($(which $cmd))"
    else
        echo -e "${RED}✗ $desc not found${NC}"
        return 1
    fi
}

# Check prerequisites
print_section "Prerequisites Check"

check_command kubectl "kubectl"
check_command docker "docker"

# Check Kubernetes connectivity
echo ""
echo -e "${YELLOW}Checking Kubernetes cluster connectivity...${NC}"
if kubectl cluster-info &> /dev/null; then
    echo -e "${GREEN}✓ Kubernetes cluster is accessible${NC}"
    kubectl cluster-info | head -3
else
    echo -e "${RED}✗ Cannot connect to Kubernetes cluster${NC}"
    echo -e "${YELLOW}Current kubeconfig context:${NC}"
    kubectl config current-context 2>/dev/null || echo "No context set"
    echo -e "${YELLOW}Available contexts:${NC}"
    kubectl config get-contexts
fi

# Check project structure
print_section "Project Structure Check"

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
K8S_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$K8S_DIR")"

echo -e "${YELLOW}Detected paths:${NC}"
echo "Script directory: $SCRIPT_DIR"
echo "K8s directory: $K8S_DIR" 
echo "Project root: $PROJECT_ROOT"
echo ""

required_files=(
    "$PROJECT_ROOT/pom.xml"
    "$PROJECT_ROOT/docker-compose.yml"
    "$K8S_DIR/base/namespace.yaml"
    "$K8S_DIR/prometheus/deployment.yaml"
    "$K8S_DIR/user-analytics-service/deployment.yaml"
    "$K8S_DIR/commerce-analytics-service/deployment.yaml"
    "$K8S_DIR/prometheus/recording-rules.yaml"
    "$K8S_DIR/prometheus/alerting-rules.yaml"
)

echo -e "${YELLOW}Checking required files:${NC}"
missing_files=()
for file in "${required_files[@]}"; do
    if [[ -f "$file" ]]; then
        echo -e "${GREEN}✓ $(basename "$file")${NC}"
    else
        echo -e "${RED}✗ $file${NC}"
        missing_files+=("$file")
    fi
done

if [[ ${#missing_files[@]} -gt 0 ]]; then
    echo ""
    echo -e "${RED}Missing files detected!${NC}"
    echo -e "${YELLOW}This may indicate:${NC}"
    echo "  1. You're running the script from the wrong directory"
    echo "  2. The project structure is incomplete"
    echo "  3. Files haven't been created yet"
fi

# Check Docker images
print_section "Docker Images Check"

images=("user-analytics-service:latest" "commerce-analytics-service:latest")

echo -e "${YELLOW}Checking Docker images:${NC}"
for image in "${images[@]}"; do
    if docker image inspect "$image" &> /dev/null; then
        echo -e "${GREEN}✓ $image exists${NC}"
        # Show image size
        size=$(docker image inspect "$image" --format='{{.Size}}' | awk '{print int($1/1024/1024) "MB"}')
        echo "   Size: $size"
    else
        echo -e "${RED}✗ $image not found${NC}"
        echo -e "${YELLOW}   Run './docker-build.sh' from project root to build${NC}"
    fi
done

# Check Kubernetes resources if namespace exists
print_section "Kubernetes Resources Check"

if kubectl get namespace "$NAMESPACE" &> /dev/null; then
    echo -e "${GREEN}✓ Namespace '$NAMESPACE' exists${NC}"
    echo ""
    
    echo -e "${YELLOW}Deployments:${NC}"
    kubectl get deployments -n "$NAMESPACE" -o wide || echo "No deployments found"
    echo ""
    
    echo -e "${YELLOW}Pods:${NC}"
    kubectl get pods -n "$NAMESPACE" -o wide || echo "No pods found"
    echo ""
    
    echo -e "${YELLOW}Services:${NC}"
    kubectl get services -n "$NAMESPACE" -o wide || echo "No services found"
    echo ""
    
    echo -e "${YELLOW}ConfigMaps:${NC}"
    kubectl get configmaps -n "$NAMESPACE" || echo "No configmaps found"
    echo ""
    
    # Check for failed pods
    failed_pods=$(kubectl get pods -n "$NAMESPACE" --field-selector=status.phase=Failed -o name 2>/dev/null || echo "")
    if [[ -n "$failed_pods" ]]; then
        echo -e "${RED}Failed pods detected:${NC}"
        echo "$failed_pods"
    fi
    
    # Check for pods not ready
    not_ready_pods=$(kubectl get pods -n "$NAMESPACE" --field-selector=status.phase=Running -o jsonpath='{.items[?(@.status.containerStatuses[0].ready==false)].metadata.name}' 2>/dev/null || echo "")
    if [[ -n "$not_ready_pods" ]]; then
        echo -e "${YELLOW}Pods not ready:${NC}"
        for pod in $not_ready_pods; do
            echo "  - $pod"
            echo -e "${YELLOW}    Status:${NC}"
            kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{.status.containerStatuses[0].state}' | jq -r 'keys[0] as $k | "\($k): \(.[$k] | .reason // .message // "Unknown")"' 2>/dev/null || echo "    Unknown status"
        done
    fi
    
else
    echo -e "${YELLOW}⚠ Namespace '$NAMESPACE' does not exist${NC}"
    echo -e "${YELLOW}Run deployment script to create it${NC}"
fi

# Check service endpoints if services exist
print_section "Service Connectivity Check"

if kubectl get namespace "$NAMESPACE" &> /dev/null; then
    services=$(kubectl get services -n "$NAMESPACE" -o name 2>/dev/null | cut -d'/' -f2)
    
    if [[ -n "$services" ]]; then
        echo -e "${YELLOW}Testing service endpoints:${NC}"
        for service in $services; do
            echo -e "${BLUE}Service: $service${NC}"
            
            # Get service details
            service_type=$(kubectl get service "$service" -n "$NAMESPACE" -o jsonpath='{.spec.type}')
            port=$(kubectl get service "$service" -n "$NAMESPACE" -o jsonpath='{.spec.ports[0].port}')
            
            echo "  Type: $service_type"
            echo "  Port: $port"
            
            # Check endpoints
            endpoints=$(kubectl get endpoints "$service" -n "$NAMESPACE" -o jsonpath='{.subsets[*].addresses[*].ip}' 2>/dev/null || echo "")
            if [[ -n "$endpoints" ]]; then
                echo -e "${GREEN}  Endpoints: $endpoints${NC}"
            else
                echo -e "${RED}  No endpoints found${NC}"
            fi
            
            echo ""
        done
    else
        echo -e "${YELLOW}No services found in namespace${NC}"
    fi
fi

# Common issues and solutions
print_section "Common Issues & Solutions"

echo -e "${YELLOW}If you're experiencing issues, try these solutions:${NC}"
echo ""

echo -e "${BLUE}1. Path/File Not Found Errors:${NC}"
echo "   • Run scripts from the project root directory"
echo "   • Use the enhanced deploy.sh script: ./k8s/scripts/deploy.sh"
echo "   • Verify project structure is complete"
echo ""

echo -e "${BLUE}2. Docker Image Issues:${NC}"
echo "   • Build images: ./docker-build.sh"
echo "   • For minikube: minikube image load user-analytics-service:latest"
echo "   • For kind: kind load docker-image user-analytics-service:latest"
echo ""

echo -e "${BLUE}3. Pod CrashLoopBackOff:${NC}"
echo "   • Check logs: kubectl logs -n $NAMESPACE <pod-name>"
echo "   • Check previous logs: kubectl logs -n $NAMESPACE <pod-name> --previous"
echo "   • Check resource limits in deployment files"
echo ""

echo -e "${BLUE}4. Services Not Accessible:${NC}"
echo "   • Use port-forward: kubectl port-forward -n $NAMESPACE svc/<service-name> <port>:<port>"
echo "   • Check service endpoints: kubectl get endpoints -n $NAMESPACE"
echo "   • Verify pods are ready and running"
echo ""

echo -e "${BLUE}5. Prometheus Rules Issues:${NC}"
echo "   • Check rule syntax: promtool check rules k8s/prometheus/recording-rules.yaml"
echo "   • Verify ConfigMaps are mounted correctly"
echo "   • Check Prometheus logs for rule loading errors"
echo ""

# Quick fix commands
print_section "Quick Fix Commands"

echo -e "${YELLOW}Quick commands to try:${NC}"
echo ""
echo -e "${BLUE}Restart failed deployments:${NC}"
echo "kubectl rollout restart deployment/prometheus -n $NAMESPACE"
echo "kubectl rollout restart deployment/user-analytics-service -n $NAMESPACE"
echo "kubectl rollout restart deployment/commerce-analytics-service -n $NAMESPACE"
echo ""

echo -e "${BLUE}Clean up and redeploy:${NC}"
echo "./k8s/scripts/cleanup.sh"
echo "./k8s/scripts/deploy.sh"
echo ""

echo -e "${BLUE}Check specific pod logs:${NC}"
echo "kubectl logs -n $NAMESPACE -l app.kubernetes.io/name=prometheus --tail=50"
echo "kubectl logs -n $NAMESPACE -l app.kubernetes.io/name=user-analytics-service --tail=50"
echo ""

echo -e "${BLUE}Force delete stuck resources:${NC}"
echo "kubectl delete pod <pod-name> -n $NAMESPACE --grace-period=0 --force"
echo "kubectl delete namespace $NAMESPACE --grace-period=0 --force"
echo ""

echo -e "${GREEN}===========================================${NC}"
echo -e "${GREEN} Troubleshooting completed${NC}"
echo -e "${GREEN}===========================================${NC}"
