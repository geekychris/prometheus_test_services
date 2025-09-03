# Prometheus Rules Documentation

This document explains the recording rules and alerting rules implemented for the Micrometer Analytics Platform.

## Overview

The platform includes two types of Prometheus rules:

1. **Recording Rules** (`recording-rules.yaml`) - Pre-compute frequently used queries to improve query performance
2. **Alerting Rules** (`alerting-rules.yaml`) - Define conditions that trigger alerts when metrics exceed thresholds

## Recording Rules

Recording rules calculate and store the results of complex queries as new time series, which improves query performance and simplifies dashboard creation.

### User Analytics Recording Rules

#### Rate-based Metrics (15s interval)
```yaml
- user:registrations:rate5m          # User registration rate over 5 minutes
- user:registrations:rate_by_source  # Registration rate grouped by source (web, mobile, etc.)
- user:logins:success_rate          # Login success percentage
- user:logins:failure_rate          # Login failure percentage
- user:sessions:avg_duration        # Average session duration
- user:request_duration:p95         # 95th percentile request duration
```

#### Aggregated Metrics (30s interval)
```yaml
- user:active_users:avg             # Average active users
- user:cache_efficiency             # Cache efficiency percentage
- user:activity_score:p95           # 95th percentile user activity score
```

### Commerce Analytics Recording Rules

#### Business Metrics (15s interval)
```yaml
- commerce:orders:rate5m                    # Order rate over 5 minutes
- commerce:orders:rate_by_payment_method    # Orders grouped by payment method
- commerce:payments:success_rate            # Payment success percentage
- commerce:payments:failure_rate            # Payment failure percentage
- commerce:cart_abandonment_rate           # Cart abandonment percentage
- commerce:order_processing_duration:p95   # 95th percentile order processing time
```

#### Financial Metrics (30s interval)
```yaml
- commerce:revenue:rate_dollars_per_second  # Revenue rate in $/second
- commerce:order_value:avg                  # Average order value
- commerce:inventory:min_levels             # Minimum inventory levels
```

### Platform-wide Recording Rules

#### SLA Metrics (60s interval)
```yaml
- sli:availability:user_service      # User service availability %
- sli:availability:commerce_service  # Commerce service availability %
- sli:latency:user_service:p95      # User service 95th percentile latency
- sli:payment_success_rate          # Payment success rate for SLA tracking
```

## Alerting Rules

Alerting rules monitor the platform and trigger notifications when problems occur.

### Alert Categories

#### 1. Infrastructure Alerts (`critical` | `warning`)

**ServiceDown** (Critical)
- Triggers when: Service is down for > 1 minute
- Impact: Service unavailable to users
- Action: Immediate investigation required

**HighMemoryUsage** (Warning)
- Triggers when: JVM heap usage > 85% for 5 minutes
- Impact: Potential memory exhaustion
- Action: Scale up or investigate memory leaks

**HighCPUUsage** (Warning)
- Triggers when: CPU usage > 80% for 5 minutes
- Impact: Performance degradation
- Action: Scale up or optimize code

#### 2. Application Performance Alerts

**HighErrorRate** (Warning/Critical)
- Warning: Error rate > 5% for 2 minutes
- Critical: Error rate > 15% for 1 minute
- Impact: Users experiencing failures
- Action: Check logs, investigate root cause

**HighResponseTime** (Warning)
- Triggers when: 95th percentile response time > 2s for 5 minutes
- Impact: Poor user experience
- Action: Performance optimization needed

#### 3. Business Logic Alerts

**LowUserRegistrationRate** (Warning)
- Triggers when: Registration rate < 0.01/s for 10 minutes
- Impact: Reduced user growth
- Action: Check registration flow

**HighUserLoginFailureRate** (Warning/Critical)
- Warning: Login failure rate > 20% for 5 minutes
- Critical: Login failure rate > 50% for 2 minutes
- Impact: Users cannot access system
- Action: Check authentication system

**HighPaymentFailureRate** (Warning/Critical)
- Warning: Payment failure rate > 15% for 5 minutes
- Critical: Payment failure rate > 30% for 2 minutes
- Impact: Revenue loss
- Action: Check payment processor

**InventoryLow** (Warning/Critical)
- Warning: Inventory < 100 units for 5 minutes
- Critical: Inventory < 50 units for 2 minutes
- Impact: Stock outages
- Action: Restock inventory

#### 4. SLA Monitoring Alerts

**UserServiceAvailabilitySLA** (Warning)
- Triggers when: Availability < 99.5% for 5 minutes
- Impact: SLA breach
- Action: Investigate service issues

**PaymentSuccessRateSLA** (Warning)
- Triggers when: Payment success rate < 98% for 5 minutes
- Impact: Business SLA breach
- Action: Investigate payment issues

#### 5. Anomaly Detection Alerts

**UnusuallyHighTraffic** (Warning)
- Triggers when: Traffic > 10x 24h average for 2 minutes
- Impact: Potential DDoS or viral growth
- Action: Scale infrastructure or investigate

**UserGrowthAnomaly** (Warning)
- Triggers when: User growth significantly deviates from 7-day average
- Impact: Unusual business pattern
- Action: Investigate marketing campaigns or issues

## Usage Examples

### Querying Recording Rules

```promql
# Check user registration rate
user:registrations:rate5m

# Monitor payment success rate
commerce:payments:success_rate

# View platform error rate
platform:error_rate

# Check SLA compliance
sli:availability:user_service
```

### Common Alert Queries

```promql
# Check active alerts
ALERTS{alertstate="firing"}

# View alert history
ALERTS{alertname="ServiceDown"}[1h]

# Monitor critical alerts
ALERTS{severity="critical", alertstate="firing"}
```

## Thresholds and Tuning

### Default Thresholds

| Metric | Warning | Critical | Rationale |
|--------|---------|----------|-----------|
| Error Rate | 5% | 15% | Industry standard for web services |
| Memory Usage | 85% | - | Leave headroom for GC |
| CPU Usage | 80% | - | Prevent performance degradation |
| Response Time | 2s | - | User experience threshold |
| Login Failure | 20% | 50% | Authentication system health |
| Payment Failure | 15% | 30% | Business impact threshold |
| Inventory | 100 units | 50 units | Supply chain management |

### Customizing Thresholds

To adjust thresholds, edit the alerting rules:

```yaml
- alert: HighErrorRate
  expr: platform:error_rate > 10  # Changed from 5 to 10
  for: 5m                         # Changed from 2m to 5m
```

### Environment-specific Tuning

For different environments, consider these adjustments:

**Development:**
- Increase thresholds (more tolerance for issues)
- Reduce alert fatigue with longer evaluation periods
- Focus on critical alerts only

**Staging:**
- Similar to production but with slightly relaxed thresholds
- Use for testing alert configurations

**Production:**
- Strict thresholds for business-critical metrics
- Immediate alerts for service outages
- Comprehensive coverage for all business KPIs

## Integration with Alertmanager

To use these alerts with Alertmanager, configure routing rules based on severity and category:

```yaml
# alertmanager.yml
route:
  group_by: ['alertname', 'category']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'default'
  routes:
  - match:
      severity: critical
    receiver: 'critical-alerts'
    group_wait: 0s
  - match:
      category: business
    receiver: 'business-alerts'

receivers:
- name: 'critical-alerts'
  slack_configs:
  - channel: '#critical-alerts'
    title: 'CRITICAL: {{ .GroupLabels.alertname }}'
    
- name: 'business-alerts'
  email_configs:
  - to: 'business-team@company.com'
    subject: 'Business Alert: {{ .GroupLabels.alertname }}'
```

## Monitoring the Rules

### Rule Evaluation

Check rule evaluation status:
```promql
# Rule evaluation duration
prometheus_rule_evaluation_duration_seconds

# Failed rule evaluations
prometheus_rule_evaluation_failures_total

# Rule group evaluations
prometheus_rule_group_duration_seconds
```

### Rule Performance

Monitor rule performance:
```promql
# Recording rule computation time
increase(prometheus_rule_evaluation_duration_seconds[5m])

# Number of series created by recording rules
prometheus_tsdb_symbol_table_size_bytes
```

## Best Practices

### Recording Rules

1. **Use consistent naming**: Follow the format `level:metric:aggregation`
2. **Choose appropriate intervals**: More frequent for critical metrics
3. **Keep rules simple**: Complex rules should be broken down
4. **Document business logic**: Include comments explaining calculations

### Alerting Rules

1. **Use appropriate severity levels**: Critical for immediate action, warning for awareness
2. **Include meaningful annotations**: Help responders understand the issue
3. **Set reasonable thresholds**: Based on historical data and business requirements
4. **Include runbook links**: Guide responders to resolution steps
5. **Test alerts regularly**: Verify they trigger appropriately

### General

1. **Version control**: Keep rules in git with proper change management
2. **Review regularly**: Update thresholds based on system evolution
3. **Monitor rule performance**: Ensure rules don't impact Prometheus performance
4. **Use labels consistently**: Enable proper alert routing and filtering

## Troubleshooting

### Common Issues

**Rules not loading:**
- Check YAML syntax
- Verify file paths in prometheus.yml
- Check Prometheus logs for errors

**Rules not evaluating:**
- Verify rule expressions are valid PromQL
- Check for missing metrics or labels
- Ensure evaluation interval is appropriate

**Alerts not firing:**
- Check alert expression returns values
- Verify `for` duration isn't too long
- Check Alertmanager configuration

**Performance issues:**
- Review rule complexity and frequency
- Check for inefficient queries
- Monitor rule evaluation duration

### Debug Commands

```bash
# Check rule configuration
promtool check rules /path/to/rules.yml

# Test rule queries
promtool query instant http://prometheus:9090 'your_rule_expression'

# Check rule status in Prometheus UI
# Go to Status > Rules
```
