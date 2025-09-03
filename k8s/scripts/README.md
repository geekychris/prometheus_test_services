# Kubernetes Deployment Scripts

This directory contains scripts to deploy, manage, and troubleshoot the Micrometer Analytics Platform on Kubernetes.

## Available Scripts

### 🚀 **deploy.sh** (Recommended)
Enhanced deployment script that can be run from any directory with auto-detection.

```bash
# Basic usage (auto-detects project location)
./k8s/scripts/deploy.sh

# Specify project directory
./k8s/scripts/deploy.sh -p /path/to/micrometer_generator

# Skip Docker image building
./k8s/scripts/deploy.sh --skip-build

# Dry run (see what would be deployed)
./k8s/scripts/deploy.sh --dry-run

# Get help
./k8s/scripts/deploy.sh --help
```

**Features:**
- ✅ Auto-detects project structure
- ✅ Comprehensive prerequisite checking  
- ✅ Works from any directory
- ✅ Smart Docker image handling
- ✅ Detailed error messages

### 📦 **deploy-all.sh** (Original)
Original deployment script that must be run from the k8s directory.

```bash
# Run from k8s directory
cd k8s
./scripts/deploy-all.sh
```

**Features:**
- ✅ Full deployment with waiting
- ✅ Automatic image building
- ✅ ServiceMonitor detection
- ⚠️ Must be run from k8s directory

### 🧹 **cleanup.sh**
Uninstalls all Kubernetes resources with various options.

```bash
# Basic cleanup
./k8s/scripts/cleanup.sh

# Keep the namespace
./k8s/scripts/cleanup.sh --keep-namespace

# Dry run cleanup
./k8s/scripts/cleanup.sh --dry-run

# Aggressive cleanup with waiting
./k8s/scripts/cleanup.sh --purge --wait

# Specify different namespace
./k8s/scripts/cleanup.sh -n my-namespace
```

**Options:**
- `--keep-namespace`: Don't delete the namespace
- `--purge`: Extra cleanup using label selectors
- `--dry-run`: Show what would be deleted
- `--wait`: Wait for resources to be fully deleted
- `-n <name>`: Specify namespace (default: micrometer-analytics)

### 🗑️ **complete-uninstall.sh**
Removes both Kubernetes resources AND Docker images.

```bash
# Complete removal
./k8s/scripts/complete-uninstall.sh

# Keep Docker images
./k8s/scripts/complete-uninstall.sh --keep-images

# Keep namespace but remove everything else
./k8s/scripts/complete-uninstall.sh --keep-namespace

# Force removal without confirmation
./k8s/scripts/complete-uninstall.sh --force --wait
```

### 🐳 **purge-docker-images.sh**
Removes only the Docker images for the analytics services.

```bash
# Remove images with confirmation
./k8s/scripts/purge-docker-images.sh

# Remove images without confirmation
./k8s/scripts/purge-docker-images.sh --force

# Preview what would be removed
./k8s/scripts/purge-docker-images.sh --dry-run
```

### 🔍 **troubleshoot.sh**
Comprehensive troubleshooting and diagnostic tool.

```bash
# Run full diagnostics
./k8s/scripts/troubleshoot.sh
```

**Checks:**
- ✅ Prerequisites (kubectl, docker)
- ✅ Kubernetes connectivity
- ✅ Project structure
- ✅ Docker images
- ✅ Kubernetes resources
- ✅ Service connectivity
- ✅ Common issues and solutions

### 🧪 **test-deployment.sh**
Verifies that the deployment is working correctly.

```bash
# Test the deployment
./k8s/scripts/test-deployment.sh
```

## Quick Start Guide

### First Time Deployment

1. **Ensure you have the prerequisites:**
   ```bash
   # Check if you have kubectl and docker
   ./k8s/scripts/troubleshoot.sh
   ```

2. **Deploy everything:**
   ```bash
   # Use the enhanced deployment script
   ./k8s/scripts/deploy.sh
   ```

3. **Verify deployment:**
   ```bash
   # Test that everything is working
   ./k8s/scripts/test-deployment.sh
   ```

4. **Access services:**
   ```bash
   # Prometheus UI
   kubectl port-forward -n micrometer-analytics svc/prometheus 9090:9090
   
   # Or via NodePort
   open http://localhost:30090
   ```

### Troubleshooting Failed Deployments

1. **Run diagnostics:**
   ```bash
   ./k8s/scripts/troubleshoot.sh
   ```

2. **Check specific issues:**
   ```bash
   # View pod logs
   kubectl logs -n micrometer-analytics -l app.kubernetes.io/name=prometheus
   
   # Check pod status
   kubectl get pods -n micrometer-analytics
   
   # Describe problematic pods
   kubectl describe pod <pod-name> -n micrometer-analytics
   ```

3. **Common fixes:**
   ```bash
   # Restart deployments
   kubectl rollout restart deployment/prometheus -n micrometer-analytics
   
   # Clean up and redeploy
   ./k8s/scripts/cleanup.sh
   ./k8s/scripts/deploy.sh
   ```

### Working with Different Environments

```bash
# Deploy to staging namespace
./k8s/scripts/deploy.sh -n staging-analytics

# Clean up staging
./k8s/scripts/cleanup.sh -n staging-analytics

# Use different project path
./k8s/scripts/deploy.sh -p /path/to/different/project
```

## Script Dependencies

### Required Tools
- `kubectl` - Kubernetes command-line tool
- `docker` - For building and managing images (optional with --skip-build)

### Required Files Structure
```
micrometer_generator/
├── pom.xml
├── docker-build.sh
├── k8s/
│   ├── base/namespace.yaml
│   ├── user-analytics-service/
│   ├── commerce-analytics-service/
│   ├── prometheus/
│   └── scripts/ (this directory)
```

## Environment Variables

The scripts use these environment variables when available:

- `KUBECONFIG` - Kubernetes config file location
- `DOCKER_BUILDKIT` - Enable Docker BuildKit (set by docker-build.sh)

## Error Handling

All scripts include comprehensive error handling:

- **Exit on error**: `set -e` ensures scripts stop on first error
- **Path validation**: Scripts verify required files exist
- **Prerequisite checking**: Tools and connectivity are verified
- **Detailed logging**: Clear error messages with suggested solutions

## Examples

### Complete Workflow Example
```bash
# 1. Diagnose current state
./k8s/scripts/troubleshoot.sh

# 2. Clean up any existing deployment
./k8s/scripts/cleanup.sh

# 3. Deploy fresh
./k8s/scripts/deploy.sh

# 4. Test deployment
./k8s/scripts/test-deployment.sh

# 5. Access Prometheus
kubectl port-forward -n micrometer-analytics svc/prometheus 9090:9090
```

### Development Workflow Example
```bash
# Quick redeploy during development
./k8s/scripts/cleanup.sh --keep-namespace
./k8s/scripts/deploy.sh

# Or with automatic image rebuild
./k8s/scripts/cleanup.sh
./k8s/scripts/deploy.sh
```

### Production-like Deployment Example
```bash
# Deploy with verification and monitoring
./k8s/scripts/deploy.sh --skip-build  # Use pre-built images
./k8s/scripts/test-deployment.sh
kubectl get pods -n micrometer-analytics -w  # Monitor startup
```

## Troubleshooting Common Issues

### "Path does not exist" errors
- Use `./k8s/scripts/deploy.sh` instead of `deploy-all.sh`
- Run from project root directory
- Check project structure with `troubleshoot.sh`

### Pod CrashLoopBackOff
- Check logs: `kubectl logs -n micrometer-analytics <pod-name>`
- Check resource limits in deployment files
- Verify Docker images are built correctly

### Service not accessible
- Use port-forward: `kubectl port-forward -n micrometer-analytics svc/<service> <port>`
- Check endpoints: `kubectl get endpoints -n micrometer-analytics`
- Verify pods are ready: `kubectl get pods -n micrometer-analytics`

### Images not found
- Build images: `./docker-build.sh` from project root
- For minikube: `minikube image load user-analytics-service:latest`
- For kind: `kind load docker-image user-analytics-service:latest`

For more detailed troubleshooting, run `./k8s/scripts/troubleshoot.sh`.
