#!/bin/bash

# Simplified Kubernetes Deploy Script for Micrometer Analytics Services  
# Works on Linux and supports both kubectl and microk8s kubectl

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to detect kubectl command
detect_kubectl() {
    if command -v microk8s.kubectl &> /dev/null; then
        echo "microk8s.kubectl"
    elif command -v kubectl &> /dev/null; then
        echo "kubectl"
    else
        echo ""
    fi
}

# Function to print usage
usage() {
    echo -e "${BLUE}Micrometer Analytics Kubernetes Deployment Script${NC}"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -h, --help           Show this help message"
    echo "  -p, --project-dir    Specify project root directory"
    echo "  --skip-build         Skip Docker image building"
    echo "  --dry-run           Show what would be deployed"
    echo ""
    echo "Examples:"
    echo "  $0                   # Deploy from current directory"
    echo "  $0 --skip-build      # Deploy without building images"
    echo ""
}

# Variables
PROJECT_DIR=""
SKIP_BUILD=false
DRY_RUN=false
KUBECTL_CMD=""
NAMESPACE="micrometer-analytics"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            exit 0
            ;;
        -p|--project-dir)
            PROJECT_DIR="$2"
            shift 2
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            exit 1
            ;;
    esac
done

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE} Micrometer Analytics K8s Deployment${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Detect kubectl command
KUBECTL_CMD=$(detect_kubectl)
if [[ -z "$KUBECTL_CMD" ]]; then
    echo -e "${RED}Error: Neither 'kubectl' nor 'microk8s.kubectl' found${NC}"
    echo -e "${YELLOW}Please install kubectl or microk8s${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Using kubectl command: $KUBECTL_CMD${NC}"

# Find project root
if [[ -n "$PROJECT_DIR" ]]; then
    if [[ ! -d "$PROJECT_DIR" ]]; then
        echo -e "${RED}Project directory not found: $PROJECT_DIR${NC}"
        exit 1
    fi
    PROJECT_ROOT="$PROJECT_DIR"
else
    # Try current directory first
    if [[ -f "pom.xml" && -d "k8s" ]]; then
        PROJECT_ROOT="$(pwd)"
    else
        # Try script's parent directories
        SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
        if [[ -f "$SCRIPT_DIR/../../pom.xml" && -d "$SCRIPT_DIR/../" ]]; then
            PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
        else
            echo -e "${RED}Could not find project root${NC}"
            echo -e "${YELLOW}Please run from project directory or use -p option${NC}"
            exit 1
        fi
    fi
fi

K8S_DIR="$PROJECT_ROOT/k8s"

echo -e "${GREEN}Project root: $PROJECT_ROOT${NC}"
echo -e "${GREEN}K8s directory: $K8S_DIR${NC}"

# Verify required files exist
required_files=(
    "$PROJECT_ROOT/pom.xml"
    "$K8S_DIR/base/namespace.yaml"
    "$K8S_DIR/prometheus/deployment.yaml"
    "$K8S_DIR/user-analytics-service/deployment.yaml"
    "$K8S_DIR/commerce-analytics-service/deployment.yaml"
)

echo -e "${YELLOW}Verifying project structure...${NC}"
for file in "${required_files[@]}"; do
    if [[ ! -f "$file" ]]; then
        echo -e "${RED}Missing: $file${NC}"
        exit 1
    fi
done
echo -e "${GREEN}✓ Project structure verified${NC}"

# Test kubectl connectivity
echo -e "${YELLOW}Testing Kubernetes connection...${NC}"
if ! $KUBECTL_CMD cluster-info &> /dev/null; then
    echo -e "${RED}Cannot connect to Kubernetes cluster${NC}"
    echo -e "${YELLOW}Please check your kubeconfig or microk8s status${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Kubernetes connection verified${NC}"

# Check Docker (optional)
if [[ "$SKIP_BUILD" == false ]]; then
    if command -v docker &> /dev/null; then
        echo -e "${GREEN}✓ Docker found${NC}"
        
        # Build images if needed
        need_build=false
        for service in user-analytics-service commerce-analytics-service; do
            if ! docker image inspect "$service:latest" &> /dev/null; then
                echo -e "${YELLOW}Image $service:latest not found${NC}"
                need_build=true
            fi
        done
        
        if [[ "$need_build" == true ]]; then
            if [[ "$DRY_RUN" == true ]]; then
                echo -e "${YELLOW}[DRY-RUN] Would build Docker images${NC}"
            else
                echo -e "${BLUE}Building Docker images...${NC}"
                cd "$PROJECT_ROOT"
                if [[ -x "./docker-build.sh" ]]; then
                    ./docker-build.sh
                else
                    echo -e "${RED}docker-build.sh not found or not executable${NC}"
                    exit 1
                fi
            fi
        else
            echo -e "${GREEN}✓ Docker images exist${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ Docker not found, skipping image build${NC}"
    fi
fi

echo ""

# Deploy to Kubernetes
if [[ "$DRY_RUN" == true ]]; then
    echo -e "${YELLOW}[DRY-RUN] Would deploy these manifests:${NC}"
    find "$K8S_DIR" -name "*.yaml" -not -path "*/scripts/*" | sort
    exit 0
fi

echo -e "${BLUE}Deploying to Kubernetes...${NC}"

# Function to apply manifests
apply_manifest() {
    local manifest="$1"
    local description="$2"
    
    echo -e "${YELLOW}Deploying $description...${NC}"
    if $KUBECTL_CMD apply -f "$manifest"; then
        echo -e "${GREEN}✓ $description deployed${NC}"
    else
        echo -e "${RED}✗ Failed to deploy $description${NC}"
        exit 1
    fi
}

# Deploy in order
cd "$K8S_DIR"

# 1. Namespace
apply_manifest "base/namespace.yaml" "Namespace"

# 2. RBAC
if [[ -f "prometheus/rbac.yaml" ]]; then
    apply_manifest "prometheus/rbac.yaml" "Prometheus RBAC"
fi

# 3. ConfigMaps
apply_manifest "user-analytics-service/configmap.yaml" "User Analytics ConfigMap"
apply_manifest "commerce-analytics-service/configmap.yaml" "Commerce Analytics ConfigMap"
apply_manifest "prometheus/configmap.yaml" "Prometheus ConfigMap"

# 4. Rules (if they exist)
if [[ -f "prometheus/recording-rules.yaml" ]]; then
    apply_manifest "prometheus/recording-rules.yaml" "Recording Rules"
fi
if [[ -f "prometheus/alerting-rules.yaml" ]]; then
    apply_manifest "prometheus/alerting-rules.yaml" "Alerting Rules"  
fi

# 5. Services
apply_manifest "user-analytics-service/service.yaml" "User Analytics Service"
apply_manifest "commerce-analytics-service/service.yaml" "Commerce Analytics Service"
apply_manifest "prometheus/service.yaml" "Prometheus Service"

# 6. Deployments
apply_manifest "user-analytics-service/deployment.yaml" "User Analytics Deployment"
apply_manifest "commerce-analytics-service/deployment.yaml" "Commerce Analytics Deployment"
apply_manifest "prometheus/deployment.yaml" "Prometheus Deployment"

# 7. ServiceMonitors (if Prometheus Operator exists)
if $KUBECTL_CMD get crd servicemonitors.monitoring.coreos.com &> /dev/null; then
    if [[ -f "prometheus/servicemonitors.yaml" ]]; then
        apply_manifest "prometheus/servicemonitors.yaml" "ServiceMonitors"
    fi
else
    echo -e "${YELLOW}Prometheus Operator not detected, skipping ServiceMonitors${NC}"
fi

echo ""
echo -e "${YELLOW}Waiting for deployments...${NC}"

# Wait for deployments to be ready
for deployment in user-analytics-service commerce-analytics-service prometheus; do
    echo -e "${BLUE}Waiting for $deployment...${NC}"
    if $KUBECTL_CMD wait --for=condition=available --timeout=300s "deployment/$deployment" -n "$NAMESPACE"; then
        echo -e "${GREEN}✓ $deployment is ready${NC}"
    else
        echo -e "${YELLOW}⚠ $deployment may not be ready yet${NC}"
    fi
done

echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN} Deployment completed!${NC}" 
echo -e "${GREEN}======================================${NC}"
echo ""

# Show status
echo -e "${BLUE}Current status:${NC}"
$KUBECTL_CMD get pods,services -n "$NAMESPACE"

echo ""
echo -e "${BLUE}Access services:${NC}"
echo "- Prometheus UI: $KUBECTL_CMD port-forward -n $NAMESPACE svc/prometheus 9090:9090"
echo "- User Analytics: $KUBECTL_CMD port-forward -n $NAMESPACE svc/user-analytics-service 8081:8081"  
echo "- Commerce Analytics: $KUBECTL_CMD port-forward -n $NAMESPACE svc/commerce-analytics-service 8082:8082"
