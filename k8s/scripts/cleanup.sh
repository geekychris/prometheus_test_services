#!/bin/bash

# Uninstall Micrometer Analytics Services from Kubernetes
# This script reverses k8s/scripts/deploy-all.sh by removing all related resources.
# It supports flags for namespace override, keeping the namespace, dry-run, and purge.

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

NAMESPACE="micrometer-analytics"
KEEP_NAMESPACE=false
PURGE=false
DRY_RUN=false
WAIT=false

usage() {
  cat <<EOF
Usage: $0 [options]

Options:
  -n, --namespace <name>   Namespace to uninstall from (default: micrometer-analytics)
  --keep-namespace         Do not delete the namespace at the end
  --purge                  Extra cleanup: delete by label selectors, try ServiceMonitor CRD, etc.
  --dry-run                Show what would be deleted without actually deleting
  --wait                   Wait for resources to be fully deleted
  -h, --help               Show this help

Examples:
  $0                                   # uninstall from default namespace
  $0 -n staging --keep-namespace       # uninstall from 'staging' but keep the namespace
  $0 --purge --wait                    # uninstall and aggressively purge related resources, waiting on deletions
EOF
}

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    -n|--namespace)
      NAMESPACE="$2"; shift 2;;
    --keep-namespace)
      KEEP_NAMESPACE=true; shift;;
    --purge)
      PURGE=true; shift;;
    --dry-run)
      DRY_RUN=true; shift;;
    --wait)
      WAIT=true; shift;;
    -h|--help)
      usage; exit 0;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"; usage; exit 1;;
  esac
done

run_cmd() {
  local cmd="$1"
  if $DRY_RUN; then
    echo -e "${YELLOW}[dry-run]${NC} $cmd"
  else
    eval "$cmd"
  fi
}

echo -e "${BLUE}Uninstalling Micrometer Analytics from namespace '${NAMESPACE}'...${NC}"

# Check kubectl
if ! command -v kubectl &> /dev/null; then
  echo -e "${RED}kubectl could not be found. Please install kubectl first.${NC}"
  exit 1
fi

# Verify namespace exists (unless purging by labels across namespaces)
if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
  echo -e "${YELLOW}Namespace '$NAMESPACE' not found. Continuing with best-effort cleanup...${NC}"
fi

# Helper to delete namespaced or cluster resources
delete_resource() {
  local resource_type=$1
  local resource_name=$2
  local scope=${3:-namespaced} # namespaced|cluster

  if [[ "$scope" == "cluster" ]]; then
    echo -e "${YELLOW}Deleting cluster resource: $resource_type/$resource_name${NC}"
    run_cmd "kubectl delete $resource_type $resource_name --ignore-not-found=true"
  else
    echo -e "${YELLOW}Deleting $resource_type/$resource_name in namespace $NAMESPACE${NC}"
    run_cmd "kubectl delete $resource_type $resource_name -n $NAMESPACE --ignore-not-found=true"
  fi
}

# Try ServiceMonitors (if CRD exists)
if kubectl get crd servicemonitors.monitoring.coreos.com &> /dev/null; then
  echo -e "${BLUE}Deleting ServiceMonitors (Prometheus Operator detected)...${NC}"
  delete_resource servicemonitor user-analytics-service-monitor
  delete_resource servicemonitor commerce-analytics-service-monitor
fi

# Delete deployments
echo -e "${BLUE}Deleting Deployments...${NC}"
delete_resource deployment user-analytics-service
delete_resource deployment commerce-analytics-service
delete_resource deployment prometheus

# Optionally wait for deletion
if $WAIT; then
  echo -e "${YELLOW}Waiting for deployments to terminate...${NC}"
  $DRY_RUN || kubectl wait --for=delete --timeout=120s deployment/user-analytics-service -n "$NAMESPACE" 2>/dev/null || true
  $DRY_RUN || kubectl wait --for=delete --timeout=120s deployment/commerce-analytics-service -n "$NAMESPACE" 2>/dev/null || true
  $DRY_RUN || kubectl wait --for=delete --timeout=120s deployment/prometheus -n "$NAMESPACE" 2>/dev/null || true
fi

# Delete services
echo -e "${BLUE}Deleting Services...${NC}"
delete_resource service user-analytics-service
delete_resource service commerce-analytics-service
delete_resource service prometheus
delete_resource service prometheus-nodeport

# Delete ConfigMaps
echo -e "${BLUE}Deleting ConfigMaps...${NC}"
delete_resource configmap user-analytics-config
delete_resource configmap commerce-analytics-config
delete_resource configmap prometheus-config
delete_resource configmap prometheus-recording-rules
delete_resource configmap prometheus-alerting-rules

# Delete RBAC
echo -e "${BLUE}Deleting RBAC resources...${NC}"
delete_resource serviceaccount prometheus-sa
# Cluster-scoped
echo -e "${YELLOW}Deleting cluster roles/bindings...${NC}"
run_cmd "kubectl delete clusterrolebinding prometheus-crb --ignore-not-found=true"
run_cmd "kubectl delete clusterrole prometheus-cr --ignore-not-found=true"

# Purge by label selector (additional safety net)
if $PURGE; then
  echo -e "${BLUE}Purging label-selected resources in namespace ${NAMESPACE}...${NC}"
  # Best-effort deletions by label
  run_cmd "kubectl delete all,cm,secret,sa,role,rolebinding --ignore-not-found=true -n $NAMESPACE -l app.kubernetes.io/part-of=analytics-platform"
  # If operator exists, purge any ServiceMonitor with labels
  if kubectl get crd servicemonitors.monitoring.coreos.com &> /dev/null; then
    run_cmd "kubectl delete servicemonitor --ignore-not-found=true -n $NAMESPACE -l app.kubernetes.io/part-of=analytics-platform"
  fi
fi

# Delete Namespace last (unless kept)
if ! $KEEP_NAMESPACE; then
  echo -e "${BLUE}Deleting Namespace '${NAMESPACE}'...${NC}"
  run_cmd "kubectl delete namespace $NAMESPACE --ignore-not-found=true"
  if $WAIT && ! $DRY_RUN; then
    echo -e "${YELLOW}Waiting for namespace to terminate...${NC}"
    kubectl wait --for=delete ns/$NAMESPACE --timeout=180s 2>/dev/null || true
  fi
else
  echo -e "${YELLOW}Skipping namespace deletion (per --keep-namespace).${NC}"
fi

# Summary and hints
echo -e "${GREEN}Uninstall completed!${NC}"
echo -e "${YELLOW}Note:${NC} Local Docker images remain. Remove if desired:
  docker image rm user-analytics-service:latest commerce-analytics-service:latest 2>/dev/null || true"
