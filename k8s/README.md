# Kubernetes Deployment Guide - Micrometer Analytics Platform

This directory contains Kubernetes manifests and deployment scripts for running the Micrometer Analytics Platform (User Analytics Service + Commerce Analytics Service + Prometheus) in a Kubernetes cluster.

## Overview

The Kubernetes deployment includes:

- **User Analytics Service**: Deployed as a scalable service with 2 replicas by default
- **Commerce Analytics Service**: Deployed as a scalable service with 2 replicas by default  
- **Prometheus**: Configured with Kubernetes service discovery to automatically scrape metrics from both services
- **Monitoring Setup**: Complete observability stack with proper RBAC, health checks, and service discovery

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                      │
│  ┌─────────────────────────────────────────────────────────┐│
│  │            Namespace: micrometer-analytics              ││
│  │                                                         ││
│  │  ┌──────────────────┐    ┌──────────────────┐          ││
│  │  │ User Analytics   │    │ Commerce         │          ││
│  │  │ Service          │    │ Analytics Service│          ││
│  │  │ (Replicas: 2)    │    │ (Replicas: 2)    │          ││
│  │  │ Port: 8081       │    │ Port: 8082       │          ││
│  │  └────────┬─────────┘    └────────┬─────────┘          ││
│  │           │                       │                    ││
│  │           └───────────┬───────────┘                    ││
│  │                       │                                ││
│  │            ┌──────────▼──────────┐                     ││
│  │            │     Prometheus      │                     ││
│  │            │   (Port: 9090)      │                     ││
│  │            │ + Service Discovery │                     ││
│  │            └─────────────────────┘                     ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

## Directory Structure

```
k8s/
├── base/
│   └── namespace.yaml                    # Kubernetes namespace
├── user-analytics-service/
│   ├── configmap.yaml                   # Configuration for User Analytics
│   ├── deployment.yaml                  # Deployment with 2 replicas
│   └── service.yaml                     # ClusterIP service
├── commerce-analytics-service/
│   ├── configmap.yaml                   # Configuration for Commerce Analytics  
│   ├── deployment.yaml                  # Deployment with 2 replicas
│   └── service.yaml                     # ClusterIP service
├── prometheus/
│   ├── rbac.yaml                        # Service Account, ClusterRole, ClusterRoleBinding
│   ├── configmap.yaml                   # Prometheus config with K8s service discovery
│   ├── recording-rules.yaml             # Recording rules for pre-computed metrics
│   ├── alerting-rules.yaml              # Alerting rules for monitoring and alerts
│   ├── deployment.yaml                  # Prometheus deployment
│   ├── service.yaml                     # ClusterIP + NodePort services
│   ├── servicemonitors.yaml             # ServiceMonitor CRDs (Prometheus Operator)
│   └── RULES.md                         # Documentation for rules and alerts
├── scripts/
│   ├── deploy-all.sh                    # Deploy everything
│   ├── cleanup.sh                       # Remove everything
│   └── test-deployment.sh               # Test deployment
└── README.md                            # This file
```

## Prerequisites

1. **Kubernetes cluster** (local or remote)
   - minikube, kind, Docker Desktop, or any Kubernetes cluster
   - `kubectl` configured to access your cluster

2. **Docker images** (automatically built by deploy script)
   - The deployment script will automatically build Docker images if they don't exist
   - Or manually run `./docker-build.sh` from the project root

3. **Optional: Prometheus Operator**
   - ServiceMonitor resources will be deployed automatically if Prometheus Operator CRDs are detected

## Quick Start

### 1. Deploy Everything

```bash
# From the project root directory
cd k8s
./scripts/deploy-all.sh
```

This script will:
- Check and build Docker images if needed
- Deploy the namespace, RBAC, ConfigMaps, Services, and Deployments
- Wait for all deployments to be ready
- Display access instructions

### 2. Verify Deployment

```bash
./scripts/test-deployment.sh
```

### 3. Access Services

After deployment, you can access the services using:

#### Option A: NodePort (Direct Access)
- **Prometheus UI**: http://localhost:30090

#### Option B: Port Forward (Recommended)
```bash
# Prometheus UI
kubectl port-forward -n micrometer-analytics svc/prometheus 9090:9090
# Visit: http://localhost:9090

# User Analytics Service
kubectl port-forward -n micrometer-analytics svc/user-analytics-service 8081:8081
# Visit: http://localhost:8081/actuator/health

# Commerce Analytics Service  
kubectl port-forward -n micrometer-analytics svc/commerce-analytics-service 8082:8082
# Visit: http://localhost:8082/actuator/health
```

### 4. Clean Up

```bash
./scripts/cleanup.sh
```

## Scaling and Replica Sets

### Understanding Prometheus Service Discovery for Replica Sets

The Prometheus configuration includes multiple service discovery mechanisms to handle replica sets effectively:

#### 1. Endpoint-Based Discovery
```yaml
- job_name: 'user-analytics-service'
  kubernetes_sd_configs:
  - role: endpoints
    namespaces:
      names: [micrometer-analytics]
```

This discovers all endpoints behind a service, automatically handling:
- Multiple replicas of the same service
- Pod scaling up/down
- Pod restarts and IP changes

#### 2. Pod-Based Discovery with Annotations
```yaml
- job_name: 'kubernetes-pods-annotated'
  kubernetes_sd_configs:
  - role: pod
```

This discovers pods directly using Prometheus annotations:
- `prometheus.io/scrape: "true"`
- `prometheus.io/path: "/actuator/prometheus"`
- `prometheus.io/port: "8081"` (or `8082`)

### Scaling Services

#### Scale Up Services
```bash
# Scale User Analytics to 5 replicas
kubectl scale deployment user-analytics-service --replicas=5 -n micrometer-analytics

# Scale Commerce Analytics to 3 replicas
kubectl scale deployment commerce-analytics-service --replicas=3 -n micrometer-analytics

# Verify scaling
kubectl get pods -n micrometer-analytics
```

#### Monitor Scaled Services in Prometheus

After scaling, Prometheus will automatically discover the new pod endpoints. You can verify this by:

1. **Check Targets**: Visit Prometheus UI → Status → Targets
   - You should see multiple targets for each service
   - Each replica will appear as a separate target

2. **Query Individual Pods**:
   ```promql
   # See metrics from all User Analytics pods
   user_registrations_total
   
   # Group by pod to see per-replica metrics
   sum by (pod) (user_registrations_total)
   
   # See metrics from all Commerce Analytics pods
   commerce_orders_total
   
   # Group by pod to see per-replica metrics
   sum by (pod) (commerce_orders_total)
   ```

3. **Aggregate Across Replicas**:
   ```promql
   # Total registrations across all User Analytics replicas
   sum(user_registrations_total)
   
   # Total orders across all Commerce Analytics replicas
   sum(commerce_orders_total)
   
   # Average response time across all replicas
   avg(rate(http_server_requests_seconds_sum[1m]) / rate(http_server_requests_seconds_count[1m]))
   ```

### Replica Set Behavior

When services are part of a replica set, Prometheus will:

1. **Automatically discover new pods** when you scale up
2. **Stop scraping pods** that are terminated during scale-down
3. **Maintain separate metrics** for each pod instance
4. **Provide labels** to distinguish between pods:
   - `pod`: Pod name (e.g., `user-analytics-service-7d4b8c9f8-abcd1`)
   - `service`: Service name (e.g., `user-analytics-service`)
   - `namespace`: Kubernetes namespace

## Configuration Details

### Service Configuration

Both services are configured for Kubernetes with:

- **Environment**: Set to `kubernetes`
- **Instance ID**: Uses pod hostname (`${HOSTNAME}`)
- **Health Checks**: Comprehensive liveness, readiness, and startup probes
- **Security**: Non-root user, read-only root filesystem, dropped capabilities
- **Resource Limits**: Memory and CPU limits for stable operation

### Prometheus Configuration

#### Service Discovery Jobs

1. **Direct Service Discovery**: Discovers services by name
   - `user-analytics-service`
   - `commerce-analytics-service`

2. **Pod Annotation Discovery**: Discovers pods with Prometheus annotations
   - Automatically handles replica sets
   - Uses pod-level labels and annotations

3. **Optional Kubernetes Cluster Monitoring**:
   - API server metrics
   - Node/kubelet metrics

#### Metrics Path and Labels

- **Path**: `/actuator/prometheus` (Spring Boot Actuator)
- **Labels Added**:
  - `service`: Service name
  - `pod`: Pod name  
  - `namespace`: Kubernetes namespace

### RBAC Configuration

Prometheus runs with minimal required permissions:

```yaml
ClusterRole permissions:
- nodes, services, endpoints, pods: get, list, watch
- /metrics, /metrics/cadvisor: get
```

## Troubleshooting

### Common Issues

#### 1. Pods Not Starting
```bash
# Check pod status
kubectl get pods -n micrometer-analytics

# Check pod logs
kubectl logs -n micrometer-analytics deployment/user-analytics-service
kubectl logs -n micrometer-analytics deployment/commerce-analytics-service

# Check events
kubectl describe pods -n micrometer-analytics
```

#### 2. Prometheus Not Discovering Services
```bash
# Check Prometheus targets
kubectl port-forward -n micrometer-analytics svc/prometheus 9090:9090
# Visit: http://localhost:9090/targets

# Check service endpoints
kubectl get endpoints -n micrometer-analytics

# Check service annotations
kubectl describe service user-analytics-service -n micrometer-analytics
```

#### 3. Services Not Responding
```bash
# Test service connectivity
kubectl run test-pod --image=curlimages/curl -i --tty --rm -- sh
# Inside the pod:
curl http://user-analytics-service.micrometer-analytics:8081/actuator/health
curl http://commerce-analytics-service.micrometer-analytics:8082/actuator/health
```

#### 4. Image Pull Issues
```bash
# Check if images exist
docker image ls | grep analytics

# Build images if missing
cd ..  # Go to project root
./docker-build.sh

# For development clusters, you might need to load images:
# minikube: minikube image load user-analytics-service:latest
# kind: kind load docker-image user-analytics-service:latest
```

### Debugging Commands

```bash
# Get all resources
kubectl get all -n micrometer-analytics

# Check resource consumption
kubectl top pods -n micrometer-analytics

# Check logs for all services
kubectl logs -f -l app.kubernetes.io/part-of=analytics-platform -n micrometer-analytics

# Port forward to specific pod (for debugging)
kubectl port-forward -n micrometer-analytics <pod-name> 8081:8081
```

## Service Monitoring and Metrics

### Available Metrics Endpoints

Once deployed, the following metrics endpoints are available:

```bash
# Via port-forward
kubectl port-forward -n micrometer-analytics svc/user-analytics-service 8081:8081
curl http://localhost:8081/actuator/prometheus

kubectl port-forward -n micrometer-analytics svc/commerce-analytics-service 8082:8082  
curl http://localhost:8082/actuator/prometheus

# Via Prometheus (aggregated)
kubectl port-forward -n micrometer-analytics svc/prometheus 9090:9090
# Visit: http://localhost:9090
```

### Key Metrics to Monitor

#### User Analytics Service
- `user_registrations_total` - User registrations by source/type
- `user_logins_total` - User logins by authentication method
- `user_sessions_total` - Active user sessions
- `user_active_count` - Current active users (gauge)
- `user_request_duration_*` - Request processing times

#### Commerce Analytics Service  
- `commerce_orders_total` - Orders by type/payment method
- `commerce_payments_total` - Payments by method/status
- `commerce_products_views_total` - Product views by category
- `commerce_inventory_levels` - Current inventory levels (gauge)
- `commerce_revenue_total` - Total revenue (gauge)

#### System Metrics (Both Services)
- `http_server_requests_*` - HTTP request metrics
- `jvm_memory_*` - JVM memory usage
- `process_cpu_usage` - CPU utilization
- `system_load_average_1m` - System load

### Sample Prometheus Queries for Replica Sets

```promql
# Request rate across all replicas
sum(rate(http_server_requests_total[1m])) by (service)

# Memory usage by pod
sum(jvm_memory_used_bytes{area="heap"}) by (pod, service)

# 95th percentile response time by service
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[1m])) by (service, le))

# Error rate by service and pod
sum(rate(http_server_requests_total{status=~"5.."}[1m])) by (service, pod) / 
sum(rate(http_server_requests_total[1m])) by (service, pod)

# Active replicas by service
count by (service) (up{job=~".*analytics.*"})
```

## Advanced Configuration

### Using with Prometheus Operator

If you have Prometheus Operator installed, ServiceMonitor resources are automatically deployed:

```bash
# Check if Prometheus Operator is available
kubectl get crd servicemonitors.monitoring.coreos.com

# If available, ServiceMonitors will be automatically deployed
# Check ServiceMonitors
kubectl get servicemonitors -n micrometer-analytics
```

### Custom Resource Limits

Edit the deployment files to adjust resource limits:

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "100m"
  limits:
    memory: "512Mi"  # Increase for high-load environments
    cpu: "500m"      # Increase for high-load environments
```

### Persistent Storage for Prometheus

By default, Prometheus uses `emptyDir` storage. For production, consider using persistent volumes:

```yaml
# In prometheus/deployment.yaml, replace emptyDir with:
volumes:
- name: data
  persistentVolumeClaim:
    claimName: prometheus-pvc
```

## Integration with Monitoring Stack

This setup integrates well with:

- **Grafana**: Create dashboards using the Prometheus data source
- **AlertManager**: Add alerting rules to Prometheus configuration  
- **Jaeger/Zipkin**: Add distributed tracing (requires application changes)
- **ELK Stack**: For log aggregation and analysis

The services are designed to be observable and integrate seamlessly with cloud-native monitoring solutions.

## Production Considerations

For production deployments, consider:

1. **Resource Limits**: Adjust based on expected load
2. **Persistent Storage**: Use persistent volumes for Prometheus data
3. **High Availability**: Run Prometheus with multiple replicas
4. **Security**: Enable HTTPS, use proper RBAC, network policies
5. **Monitoring**: Set up alerting for service health and performance
6. **Backup**: Regular backups of Prometheus data and configurations
