#!/bin/bash

# Complete Uninstall of Micrometer Analytics Platform
# This script is the complete reverse of deploy-all.sh, removing both Kubernetes resources and Docker images

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

NAMESPACE="micrometer-analytics"
KEEP_NAMESPACE=false
KEEP_IMAGES=false
PURGE=false
DRY_RUN=false
WAIT=false
FORCE=false

usage() {
  cat <<EOF
Usage: $0 [options]

Complete uninstall of Micrometer Analytics Platform (Kubernetes + Docker).
This script reverses everything done by deploy-all.sh.

Options:
  -n, --namespace <name>   Namespace to uninstall from (default: micrometer-analytics)
  --keep-namespace         Do not delete the Kubernetes namespace
  --keep-images            Do not remove Docker images  
  --purge                  Extra cleanup: delete by label selectors, etc.
  --dry-run                Show what would be deleted without actually deleting
  --wait                   Wait for resources to be fully deleted
  --force                  Skip confirmation prompts
  -h, --help               Show this help

Examples:
  $0                                   # complete uninstall with confirmation
  $0 --dry-run                         # see what would be removed
  $0 --force --wait                    # uninstall everything, wait for completion
  $0 --keep-images --keep-namespace    # remove only K8s resources, keep namespace and images
  $0 -n staging --purge                # uninstall from staging namespace with extra cleanup
EOF
}

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    -n|--namespace)
      NAMESPACE="$2"; shift 2;;
    --keep-namespace)
      KEEP_NAMESPACE=true; shift;;
    --keep-images)
      KEEP_IMAGES=true; shift;;
    --purge)
      PURGE=true; shift;;
    --dry-run)
      DRY_RUN=true; shift;;
    --wait)
      WAIT=true; shift;;
    --force)
      FORCE=true; shift;;
    -h|--help)
      usage; exit 0;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"; usage; exit 1;;
  esac
done

echo -e "${BLUE}=== Complete Micrometer Analytics Uninstall ===${NC}"
echo -e "Namespace: ${YELLOW}$NAMESPACE${NC}"
echo -e "Keep namespace: ${YELLOW}$KEEP_NAMESPACE${NC}"  
echo -e "Keep Docker images: ${YELLOW}$KEEP_IMAGES${NC}"
echo -e "Purge mode: ${YELLOW}$PURGE${NC}"
echo -e "Dry run: ${YELLOW}$DRY_RUN${NC}"
echo -e "Wait for deletions: ${YELLOW}$WAIT${NC}"

if ! $FORCE && ! $DRY_RUN; then
  echo ""
  echo -e "${YELLOW}This will remove:${NC}"
  echo "  • Kubernetes deployments, services, configmaps in namespace '$NAMESPACE'"
  echo "  • Prometheus RBAC (cluster-scoped)"
  if ! $KEEP_NAMESPACE; then
    echo "  • Kubernetes namespace '$NAMESPACE'"
  fi
  if ! $KEEP_IMAGES; then
    echo "  • Docker images: user-analytics-service:latest, commerce-analytics-service:latest"
  fi
  echo ""
  echo -e "${RED}Are you sure you want to proceed? [y/N]${NC}"
  read -r response
  if [[ ! "$response" =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}Aborted by user.${NC}"
    exit 0
  fi
fi

echo ""

# Step 1: Kubernetes cleanup
echo -e "${BLUE}Step 1: Kubernetes cleanup${NC}"
k8s_args=()
k8s_args+=(-n "$NAMESPACE")
if $KEEP_NAMESPACE; then
  k8s_args+=(--keep-namespace)
fi
if $PURGE; then
  k8s_args+=(--purge)
fi
if $DRY_RUN; then
  k8s_args+=(--dry-run)
fi
if $WAIT; then
  k8s_args+=(--wait)
fi

"$SCRIPT_DIR/cleanup.sh" "${k8s_args[@]}"

echo ""

# Step 2: Docker image cleanup
if ! $KEEP_IMAGES; then
  echo -e "${BLUE}Step 2: Docker image cleanup${NC}"
  docker_args=()
  if $DRY_RUN; then
    docker_args+=(--dry-run)
  else
    docker_args+=(--force)  # Skip confirmation since we already confirmed above
  fi
  
  "$SCRIPT_DIR/purge-docker-images.sh" "${docker_args[@]}"
else
  echo -e "${YELLOW}Step 2: Skipping Docker image cleanup (per --keep-images)${NC}"
fi

echo ""

# Summary
if $DRY_RUN; then
  echo -e "${GREEN}=== Dry Run Complete ===${NC}"
  echo -e "Run without ${YELLOW}--dry-run${NC} to perform the actual uninstall."
else
  echo -e "${GREEN}=== Complete Uninstall Finished ===${NC}"
  
  # Verify cleanup
  echo ""
  echo -e "${BLUE}Verification:${NC}"
  
  # Check if namespace still exists
  if kubectl get namespace "$NAMESPACE" &> /dev/null; then
    if $KEEP_NAMESPACE; then
      echo -e "${GREEN}✓ Namespace '$NAMESPACE' kept as requested${NC}"
      # Show remaining resources
      remaining_resources=$(kubectl get all -n "$NAMESPACE" --no-headers 2>/dev/null | wc -l)
      if [ "$remaining_resources" -gt 0 ]; then
        echo -e "${YELLOW}⚠ $remaining_resources resource(s) still in namespace${NC}"
      else
        echo -e "${GREEN}✓ No resources remaining in namespace${NC}"
      fi
    else
      echo -e "${YELLOW}⚠ Namespace '$NAMESPACE' still exists (may be terminating)${NC}"
    fi
  else
    echo -e "${GREEN}✓ Namespace '$NAMESPACE' removed${NC}"
  fi
  
  # Check cluster-scoped resources
  if kubectl get clusterrole prometheus-cr &> /dev/null; then
    echo -e "${YELLOW}⚠ ClusterRole 'prometheus-cr' still exists${NC}"
  else
    echo -e "${GREEN}✓ ClusterRole 'prometheus-cr' removed${NC}"
  fi
  
  # Check Docker images
  if ! $KEEP_IMAGES; then
    remaining_images=0
    if docker image inspect user-analytics-service:latest &> /dev/null; then
      ((remaining_images++))
    fi
    if docker image inspect commerce-analytics-service:latest &> /dev/null; then
      ((remaining_images++))
    fi
    
    if [ $remaining_images -eq 0 ]; then
      echo -e "${GREEN}✓ All Docker images removed${NC}"
    else
      echo -e "${YELLOW}⚠ $remaining_images Docker image(s) still exist${NC}"
    fi
  fi
fi
