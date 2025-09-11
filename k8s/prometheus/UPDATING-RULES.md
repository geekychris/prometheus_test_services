# Updating Prometheus Rules

This guide explains how to update Prometheus recording rules and alerting rules in Kubernetes, and ensure Prometheus recognizes the changes.

## Overview

Prometheus rules in Kubernetes are stored as ConfigMaps. When you update the rules, you need to:
1. Update the ConfigMap with new rules
2. Reload Prometheus configuration to recognize the changes
3. Verify the rules are loaded correctly

## Rule Files Location

- **Recording Rules**: `k8s/prometheus/recording-rules.yaml`
- **Alerting Rules**: `k8s/prometheus/alerting-rules.yaml`
- **Documentation**: `k8s/prometheus/RULES.md`

## Quick Update Process

### Using the Update Script (Recommended)

The easiest way to update rules is using the provided script:

```bash
# Update both recording and alerting rules
./k8s/scripts/update-rules.sh

# Update only recording rules
./k8s/scripts/update-rules.sh -r k8s/prometheus/recording-rules.yaml

# Validate rules without applying
./k8s/scripts/update-rules.sh --validate-only

# Update and wait for reload to complete
./k8s/scripts/update-rules.sh --wait
```

### Manual Process

If you prefer manual control:

```bash
# 1. Edit the rule files
vi k8s/prometheus/recording-rules.yaml
vi k8s/prometheus/alerting-rules.yaml

# 2. Apply ConfigMaps
kubectl apply -f k8s/prometheus/recording-rules.yaml
kubectl apply -f k8s/prometheus/alerting-rules.yaml

# 3. Reload Prometheus
kubectl exec -n micrometer-analytics $(kubectl get pods -n micrometer-analytics -l app.kubernetes.io/name=prometheus -o jsonpath='{.items[0].metadata.name}') -- kill -HUP 1
```

## Detailed Step-by-Step Process

### Step 1: Edit Rule Files

#### Recording Rules Format
Recording rules are stored in `recording-rules.yaml` as a Kubernetes ConfigMap:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-recording-rules
  namespace: micrometer-analytics
data:
  recording_rules.yml: |
    groups:
    - name: user_analytics_rates
      interval: 15s
      rules:
      - record: user:registrations:rate5m
        expr: rate(user_registrations_total[5m])
      - record: user:logins:success_rate
        expr: rate(user_logins_total{success="true"}[5m]) / rate(user_logins_total[5m]) * 100
```

#### Alerting Rules Format
Alerting rules are stored in `alerting-rules.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-alerting-rules
  namespace: micrometer-analytics
data:
  alerting_rules.yml: |
    groups:
    - name: infrastructure_alerts
      rules:
      - alert: ServiceDown
        expr: up{job=~".*analytics.*"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "{{ $labels.job }} service is down"
```

### Step 2: Validate Rules

Before applying rules, validate their syntax:

#### Using promtool (if available)
```bash
# Install Prometheus tools
# macOS: brew install prometheus
# Linux: Download from https://prometheus.io/download/

# Extract rules from ConfigMap and validate
python3 -c "
import yaml
with open('k8s/prometheus/recording-rules.yaml', 'r') as f:
    cm = yaml.safe_load(f)
    with open('/tmp/recording_rules.yml', 'w') as out:
        out.write(cm['data']['recording_rules.yml'])
"

promtool check rules /tmp/recording_rules.yml
```

#### Using the update script
```bash
# Validate without applying
./k8s/scripts/update-rules.sh --validate-only
```

### Step 3: Apply ConfigMap Updates

#### Update ConfigMaps
```bash
# Apply recording rules
kubectl apply -f k8s/prometheus/recording-rules.yaml

# Apply alerting rules  
kubectl apply -f k8s/prometheus/alerting-rules.yaml

# Verify ConfigMaps are updated
kubectl get configmaps -n micrometer-analytics | grep prometheus
```

#### Check ConfigMap Content
```bash
# View recording rules ConfigMap
kubectl get configmap prometheus-recording-rules -n micrometer-analytics -o yaml

# View alerting rules ConfigMap
kubectl get configmap prometheus-alerting-rules -n micrometer-analytics -o yaml
```

### Step 4: Reload Prometheus Configuration

Prometheus needs to be told to reload its configuration after ConfigMap changes.

#### Method 1: HTTP Reload Endpoint (Preferred)
```bash
# Get Prometheus pod name
PROMETHEUS_POD=$(kubectl get pods -n micrometer-analytics -l app.kubernetes.io/name=prometheus -o jsonpath='{.items[0].metadata.name}')

# Trigger reload via HTTP (requires --web.enable-lifecycle flag)
kubectl exec -n micrometer-analytics $PROMETHEUS_POD -- wget --post-data='' http://localhost:9090/-/reload

# Or using curl
kubectl exec -n micrometer-analytics $PROMETHEUS_POD -- curl -X POST http://localhost:9090/-/reload
```

#### Method 2: SIGHUP Signal
```bash
# Send SIGHUP signal to Prometheus process
kubectl exec -n micrometer-analytics $PROMETHEUS_POD -- kill -HUP 1
```

#### Method 3: Pod Restart (Last Resort)
```bash
# Delete pod to force restart (Kubernetes will recreate it)
kubectl delete pod $PROMETHEUS_POD -n micrometer-analytics
```

### Step 5: Verify Rules are Loaded

#### Check via Prometheus API
```bash
# Check if rules are loaded
kubectl exec -n micrometer-analytics $PROMETHEUS_POD -- wget -q -O- http://localhost:9090/api/v1/rules | jq '.data.groups[].rules[] | {name: .name, type: .type}'

# Count loaded rules
kubectl exec -n micrometer-analytics $PROMETHEUS_POD -- wget -q -O- http://localhost:9090/api/v1/rules | jq '.data.groups[].rules | length'
```

#### Check via Prometheus UI
1. Access Prometheus UI:
   ```bash
   kubectl port-forward -n micrometer-analytics svc/prometheus 9090:9090
   ```
2. Visit http://localhost:9090
3. Go to **Status → Rules** to see loaded rules
4. Go to **Status → Configuration** to see current config

#### Test Recording Rules
```bash
# Query recording rules to verify they're working
kubectl exec -n micrometer-analytics $PROMETHEUS_POD -- wget -q -O- 'http://localhost:9090/api/v1/query?query=user:registrations:rate5m'

# Or via port-forward and local curl
curl 'http://localhost:9090/api/v1/query?query=user:registrations:rate5m'
```

## Common Issues and Solutions

### Issue 1: Rules Not Loading

**Symptoms:**
- Rules don't appear in Status → Rules
- Recording rule queries return no data
- Alerts not firing

**Solutions:**
1. Check ConfigMap syntax:
   ```bash
   kubectl get configmap prometheus-recording-rules -n micrometer-analytics -o yaml
   ```

2. Check Prometheus logs for errors:
   ```bash
   kubectl logs -n micrometer-analytics -l app.kubernetes.io/name=prometheus
   ```

3. Verify rule syntax with promtool:
   ```bash
   ./k8s/scripts/update-rules.sh --validate-only
   ```

### Issue 2: Prometheus Not Reloading

**Symptoms:**
- ConfigMap updated but rules unchanged
- Old rules still active

**Solutions:**
1. Check if `--web.enable-lifecycle` is enabled in Prometheus args
2. Use SIGHUP signal instead of HTTP endpoint
3. Restart Prometheus pod as last resort

### Issue 3: Invalid Rule Syntax

**Symptoms:**
- Prometheus logs show rule parsing errors
- Rules page shows syntax errors

**Solutions:**
1. Validate PromQL expressions:
   ```bash
   # Test expression in Prometheus UI Query page
   rate(user_registrations_total[5m])
   ```

2. Check YAML indentation and structure
3. Use promtool to validate rules before applying

### Issue 4: Rules Evaluating but No Data

**Symptoms:**
- Rules appear in Status → Rules
- Queries return empty results

**Solutions:**
1. Check if underlying metrics exist:
   ```bash
   curl 'http://localhost:9090/api/v1/query?query=user_registrations_total'
   ```

2. Verify metric labels and selectors match
3. Check rule evaluation frequency vs data availability

## Advanced Topics

### Rule Groups and Evaluation Order

Rules are organized into groups that evaluate sequentially:

```yaml
groups:
- name: base_rates
  interval: 15s
  rules:
  - record: user:logins:rate5m
    expr: rate(user_logins_total[5m])

- name: derived_metrics  # Evaluates after base_rates
  interval: 30s
  rules:
  - record: user:logins:success_rate
    expr: user:logins:rate5m{success="true"} / user:logins:rate5m * 100
```

### Rule Dependencies

When rules depend on other rules:
1. Put base rules in earlier groups
2. Use appropriate evaluation intervals
3. Consider rule evaluation delays

### Performance Considerations

1. **Rule Frequency**: Don't evaluate rules more frequently than needed
2. **Rule Complexity**: Complex rules consume more resources
3. **Rule Count**: Too many rules can impact Prometheus performance

### Multi-Environment Rules

For different environments, consider:

```yaml
# Environment-specific recording rules
- record: user:logins:rate5m
  expr: rate(user_logins_total[5m])
  labels:
    env: '{{ $labels.environment }}'
```

## Automation and CI/CD

### Git Workflow
1. Update rule files in version control
2. Use CI/CD to validate rules with promtool
3. Apply ConfigMaps via kubectl in deployment pipeline
4. Trigger Prometheus reload automatically

### Rule Testing
```bash
# Test recording rules
./k8s/scripts/update-rules.sh --validate-only

# Integration test with temporary deployment
kubectl apply -f k8s/prometheus/recording-rules.yaml --dry-run=client

# Load test rule evaluation performance
curl 'http://localhost:9090/api/v1/query?query=prometheus_rule_evaluation_duration_seconds'
```

## Monitoring Rule Performance

### Key Metrics to Monitor
```promql
# Rule evaluation duration
prometheus_rule_evaluation_duration_seconds

# Failed rule evaluations
prometheus_rule_evaluation_failures_total

# Rule group evaluation duration
prometheus_rule_group_duration_seconds

# Number of rules by type
prometheus_rule_group_rules
```

### Performance Optimization
1. Use appropriate evaluation intervals
2. Optimize PromQL expressions
3. Monitor rule evaluation times
4. Consider recording rule chains vs complex expressions

## Examples

### Adding a New Recording Rule

1. Edit `k8s/prometheus/recording-rules.yaml`:
   ```yaml
   # Add to existing group
   - record: commerce:conversion_rate
     expr: |
       rate(commerce_orders_total[5m]) / 
       rate(commerce_cart_actions_total{action="add_item"}[5m]) * 100
   ```

2. Apply and reload:
   ```bash
   ./k8s/scripts/update-rules.sh
   ```

3. Test the new rule:
   ```bash
   curl 'http://localhost:9090/api/v1/query?query=commerce:conversion_rate'
   ```

### Adding a New Alert Rule

1. Edit `k8s/prometheus/alerting-rules.yaml`:
   ```yaml
   - alert: LowConversionRate
     expr: commerce:conversion_rate < 5
     for: 10m
     labels:
       severity: warning
       category: business
     annotations:
       summary: "Low e-commerce conversion rate"
       description: "Conversion rate is {{ $value }}% (threshold: 5%)"
   ```

2. Apply and verify:
   ```bash
   ./k8s/scripts/update-rules.sh --wait
   ```

## Best Practices

1. **Version Control**: Keep all rule changes in Git
2. **Validation**: Always validate rules before applying
3. **Testing**: Test rules in development before production
4. **Documentation**: Document rule purpose and thresholds
5. **Monitoring**: Monitor rule evaluation performance
6. **Gradual Rollout**: Deploy rule changes incrementally
7. **Rollback Plan**: Keep previous rule versions for quick rollback

## Quick Reference

### Common Commands
```bash
# Update all rules
./k8s/scripts/update-rules.sh

# Validate only
./k8s/scripts/update-rules.sh --validate-only

# Manual reload
kubectl exec -n micrometer-analytics $(kubectl get pods -n micrometer-analytics -l app.kubernetes.io/name=prometheus -o jsonpath='{.items[0].metadata.name}') -- kill -HUP 1

# Check loaded rules
curl 'http://localhost:9090/api/v1/rules'

# Test recording rule
curl 'http://localhost:9090/api/v1/query?query=user:registrations:rate5m'
```

### File Locations
- Recording rules: `k8s/prometheus/recording-rules.yaml`
- Alerting rules: `k8s/prometheus/alerting-rules.yaml`
- Update script: `k8s/scripts/update-rules.sh`
- Documentation: `k8s/prometheus/RULES.md`
