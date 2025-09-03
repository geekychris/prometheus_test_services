#!/bin/bash

# Enhanced Deploy Script for Micrometer Analytics Services
# This script can be run from any directory and will auto-detect the project structure

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print usage
usage() {
    echo -e "${BLUE}Micrometer Analytics Kubernetes Deployment Script${NC}"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -h, --help           Show this help message"
    echo "  -p, --project-dir    Specify project root directory (auto-detected by default)"
    echo "  --skip-build         Skip Docker image building"
    echo "  --dry-run           Show what would be deployed without actually deploying"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Deploy from auto-detected project"
    echo "  $0 -p /path/to/micrometer_generator  # Deploy from specific project path"
    echo "  $0 --skip-build                      # Deploy without building images"
    echo ""
}

# Variables
PROJECT_DIR=""
SKIP_BUILD=false
DRY_RUN=false

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

# Function to find project root directory
find_project_root() {
    local current_dir="$(pwd)"
    local search_dir="$current_dir"
    
    # If user specified project directory, use it
    if [[ -n "$PROJECT_DIR" ]]; then
        if [[ ! -d "$PROJECT_DIR" ]]; then
            echo -e "${RED}Specified project directory does not exist: $PROJECT_DIR${NC}"
            exit 1
        fi
        echo "$PROJECT_DIR"
        return
    fi
    
    # Auto-detect by looking for characteristic files
    for i in {1..5}; do
        if [[ -f "$search_dir/pom.xml" ]] && [[ -d "$search_dir/k8s" ]] && [[ -d "$search_dir/user-analytics-service" ]]; then
            echo "$search_dir"
            return
        fi
        search_dir="$(dirname "$search_dir")"
        if [[ "$search_dir" == "/" ]]; then
            break
        fi
    done
    
    # Check if script is in the expected location
    local script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
    local k8s_dir="$(dirname "$script_dir")"
    local potential_project_dir="$(dirname "$k8s_dir")"
    
    if [[ -f "$potential_project_dir/pom.xml" ]] && [[ -d "$potential_project_dir/k8s" ]]; then
        echo "$potential_project_dir"
        return
    fi
    
    echo ""
}

# Main execution
echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE} Micrometer Analytics K8s Deployment${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# Find project root
PROJECT_ROOT=$(find_project_root)

if [[ -z "$PROJECT_ROOT" ]]; then
    echo -e "${RED}Could not auto-detect project root directory!${NC}"
    echo ""
    echo -e "${YELLOW}Please ensure you are running this script from within the micrometer_generator project${NC}"
    echo -e "${YELLOW}or specify the project directory with: $0 -p /path/to/micrometer_generator${NC}"
    echo ""
    echo -e "${YELLOW}The project root should contain:${NC}"
    echo "  - pom.xml"
    echo "  - k8s/ directory"
    echo "  - user-analytics-service/ directory"
    echo "  - commerce-analytics-service/ directory"
    exit 1
fi

K8S_DIR="$PROJECT_ROOT/k8s"

echo -e "${GREEN}Project root detected: $PROJECT_ROOT${NC}"
echo -e "${GREEN}K8s manifests directory: $K8S_DIR${NC}"
echo ""

# Verify project structure
echo -e "${YELLOW}Verifying project structure...${NC}"

required_files=(
    "$PROJECT_ROOT/pom.xml"
    "$K8S_DIR/base/namespace.yaml"
    "$K8S_DIR/prometheus/deployment.yaml"
    "$K8S_DIR/user-analytics-service/deployment.yaml"
    "$K8S_DIR/commerce-analytics-service/deployment.yaml"
)

missing_files=()
for file in "${required_files[@]}"; do
    if [[ ! -f "$file" ]]; then
        missing_files+=("$file")
    fi
done

if [[ ${#missing_files[@]} -gt 0 ]]; then
    echo -e "${RED}Missing required files:${NC}"
    for file in "${missing_files[@]}"; do
        echo -e "${RED}  - $file${NC}"
    done
    exit 1
fi

echo -e "${GREEN}✓ Project structure verified${NC}"

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}✗ kubectl not found. Please install kubectl first.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ kubectl found${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}⚠ docker not found. Will skip image building if needed.${NC}"
    SKIP_BUILD=true
else
    echo -e "${GREEN}✓ docker found${NC}"
fi

# Check Kubernetes cluster connectivity
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}✗ Cannot connect to Kubernetes cluster. Please check your kubeconfig.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Kubernetes cluster connection verified${NC}"

echo ""

# Build Docker images if needed
if [[ "$SKIP_BUILD" == false ]]; then
    echo -e "${YELLOW}Checking Docker images...${NC}"
    
    need_build=false
    if ! docker image inspect user-analytics-service:latest &> /dev/null; then
        echo -e "${YELLOW}user-analytics-service:latest not found${NC}"
        need_build=true
    fi
    
    if ! docker image inspect commerce-analytics-service:latest &> /dev/null; then
        echo -e "${YELLOW}commerce-analytics-service:latest not found${NC}"
        need_build=true
    fi
    
    if [[ "$need_build" == true ]]; then
        if [[ "$DRY_RUN" == true ]]; then
            echo -e "${YELLOW}[DRY-RUN] Would build Docker images${NC}"
        else
            echo -e "${BLUE}Building Docker images...${NC}"
            cd "$PROJECT_ROOT"
            if [[ ! -f "./docker-build.sh" ]]; then
                echo -e "${RED}docker-build.sh not found in project root${NC}"
                exit 1
            fi
            ./docker-build.sh || { echo -e "${RED}Failed to build Docker images${NC}"; exit 1; }
        fi
    else
        echo -e "${GREEN}✓ Docker images already exist${NC}"
    fi
fi

echo ""

# Deploy to Kubernetes
if [[ "$DRY_RUN" == true ]]; then
    echo -e "${YELLOW}[DRY-RUN] Would deploy the following manifests:${NC}"
    find "$K8S_DIR" -name "*.yaml" -not -path "*/scripts/*" | sort
    exit 0
fi

echo -e "${BLUE}Deploying to Kubernetes...${NC}"

# Use the existing deploy-all.sh script with the detected project root
cd "$PROJECT_ROOT"
export PROJECT_ROOT K8S_DIR
./k8s/scripts/deploy-all.sh

echo ""
echo -e "${GREEN}===========================================${NC}"
echo -e "${GREEN} Deployment completed successfully!${NC}"
echo -e "${GREEN}===========================================${NC}"
