#!/bin/bash

# Deploy Micrometer Analytics Services to Kubernetes
# This script deploys the User Analytics Service, Commerce Analytics Service, and Prometheus

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
K8S_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$K8S_DIR")"

echo -e "${BLUE}Deploying Micrometer Analytics Services to Kubernetes...${NC}"
echo "Project Root: $PROJECT_ROOT"
echo "K8s Config Dir: $K8S_DIR"

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}kubectl could not be found. Please install kubectl first.${NC}"
    exit 1
fi

# Check if Docker images exist
echo -e "${YELLOW}Checking if Docker images exist...${NC}"
if ! docker image inspect user-analytics-service:latest &> /dev/null; then
    echo -e "${YELLOW}user-analytics-service:latest not found. Building Docker images...${NC}"
    cd "$PROJECT_ROOT"
    ./docker-build.sh || { echo -e "${RED}Failed to build Docker images${NC}"; exit 1; }
fi

if ! docker image inspect commerce-analytics-service:latest &> /dev/null; then
    echo -e "${YELLOW}commerce-analytics-service:latest not found. Building Docker images...${NC}"
    cd "$PROJECT_ROOT"
    ./docker-build.sh || { echo -e "${RED}Failed to build Docker images${NC}"; exit 1; }
fi

# Verify K8S_DIR exists and contains expected files
if [ ! -d "$K8S_DIR" ]; then
    echo -e "${RED}K8s directory not found: $K8S_DIR${NC}"
    exit 1
fi

if [ ! -f "$K8S_DIR/base/namespace.yaml" ]; then
    echo -e "${RED}Required file not found: $K8S_DIR/base/namespace.yaml${NC}"
    echo -e "${YELLOW}Current working directory: $(pwd)${NC}"
    echo -e "${YELLOW}K8S_DIR contents:${NC}"
    ls -la "$K8S_DIR" || echo "Cannot list K8S_DIR"
    exit 1
fi

echo -e "${YELLOW}Changing to K8s directory: $K8S_DIR${NC}"
cd "$K8S_DIR"
echo -e "${YELLOW}Current working directory: $(pwd)${NC}"

# Function to apply manifests with error handling
apply_manifest() {
    local manifest_path="$1"
    local description="$2"
    
    # Use absolute path to be safe
    local full_path="$K8S_DIR/$manifest_path"
    
    echo -e "${BLUE}Deploying $description...${NC}"
    echo -e "${YELLOW}Manifest path: $full_path${NC}"
    
    if [ ! -f "$full_path" ]; then
        echo -e "${RED}✗ Manifest file not found: $full_path${NC}"
        exit 1
    fi
    
    if kubectl apply -f "$full_path"; then
        echo -e "${GREEN}✓ $description deployed successfully${NC}"
    else
        echo -e "${RED}✗ Failed to deploy $description${NC}"
        exit 1
    fi
}

# Deploy namespace first
apply_manifest "base/namespace.yaml" "Namespace"

# Deploy Prometheus RBAC
apply_manifest "prometheus/rbac.yaml" "Prometheus RBAC"

# Deploy ConfigMaps
apply_manifest "user-analytics-service/configmap.yaml" "User Analytics ConfigMap"
apply_manifest "commerce-analytics-service/configmap.yaml" "Commerce Analytics ConfigMap"
apply_manifest "prometheus/configmap.yaml" "Prometheus ConfigMap"
apply_manifest "prometheus/recording-rules.yaml" "Prometheus Recording Rules"
apply_manifest "prometheus/alerting-rules.yaml" "Prometheus Alerting Rules"

# Deploy Services
apply_manifest "user-analytics-service/service.yaml" "User Analytics Service"
apply_manifest "commerce-analytics-service/service.yaml" "Commerce Analytics Service"
apply_manifest "prometheus/service.yaml" "Prometheus Service"

# Deploy Applications
apply_manifest "user-analytics-service/deployment.yaml" "User Analytics Deployment"
apply_manifest "commerce-analytics-service/deployment.yaml" "Commerce Analytics Deployment"
apply_manifest "prometheus/deployment.yaml" "Prometheus Deployment"

# Optionally deploy ServiceMonitors if Prometheus Operator is available
echo -e "${YELLOW}Checking if Prometheus Operator CRDs are available...${NC}"
if kubectl get crd servicemonitors.monitoring.coreos.com &> /dev/null; then
    echo -e "${BLUE}Prometheus Operator detected. Deploying ServiceMonitors...${NC}"
    apply_manifest "prometheus/servicemonitors.yaml" "ServiceMonitors"
else
    echo -e "${YELLOW}Prometheus Operator not detected. Skipping ServiceMonitors.${NC}"
    echo -e "${YELLOW}ServiceMonitors can be deployed later if Prometheus Operator is installed.${NC}"
fi

echo -e "${GREEN}All services deployed successfully!${NC}"

# Wait for deployments to be ready
echo -e "${YELLOW}Waiting for deployments to be ready...${NC}"
kubectl wait --for=condition=available --timeout=300s deployment/user-analytics-service -n micrometer-analytics
kubectl wait --for=condition=available --timeout=300s deployment/commerce-analytics-service -n micrometer-analytics
kubectl wait --for=condition=available --timeout=300s deployment/prometheus -n micrometer-analytics

echo -e "${GREEN}All deployments are ready!${NC}"

# Display service information
echo -e "${BLUE}Service Information:${NC}"
kubectl get pods,services -n micrometer-analytics

echo ""
echo -e "${GREEN}Deployment completed successfully!${NC}"
echo ""
echo -e "${BLUE}Access the services:${NC}"
echo "- Prometheus UI: http://localhost:30090 (via NodePort)"
echo "- Or use port-forward for Prometheus: kubectl port-forward -n micrometer-analytics svc/prometheus 9090:9090"
echo "- User Analytics Service: kubectl port-forward -n micrometer-analytics svc/user-analytics-service 8081:8081"
echo "- Commerce Analytics Service: kubectl port-forward -n micrometer-analytics svc/commerce-analytics-service 8082:8082"
echo ""
echo -e "${YELLOW}To scale the services (useful for testing replica sets):${NC}"
echo "kubectl scale deployment user-analytics-service --replicas=3 -n micrometer-analytics"
echo "kubectl scale deployment commerce-analytics-service --replicas=3 -n micrometer-analytics"
