# Prometheus Query Examples

This document contains practical Prometheus query expressions for monitoring the User Analytics Service and Commerce Analytics Service.

## Table of Contents
- [User Analytics Service Queries](#user-analytics-service-queries)
- [Commerce Analytics Service Queries](#commerce-analytics-service-queries)
- [Cross-Service Queries](#cross-service-queries)
- [Alerting Queries](#alerting-queries)
- [Dashboard Queries](#dashboard-queries)

---

## User Analytics Service Queries

### User Registration Metrics

```promql
# User registration rate (registrations per minute)
rate(user_registrations_total[1m]) * 60

# Total user registrations over time
user_registrations_total

# Registration rate by source
sum by (source) (rate(user_registrations_total[1m])) * 60

# Registration rate by user type
sum by (user_type) (rate(user_registrations_total[1m])) * 60

# Registration rate by device
sum by (device) (rate(user_registrations_total[1m])) * 60
```

### User Authentication Metrics

```promql
# User login rate (logins per minute)
rate(user_logins_total[1m]) * 60

# Login success rate percentage
(rate(user_logins_total{success="true"}[1m]) / rate(user_logins_total[1m])) * 100

# Login failure rate percentage
(rate(user_logins_total{success="false"}[1m]) / rate(user_logins_total[1m])) * 100

# Authentication latency percentiles
user_auth_duration_seconds{quantile="0.50"}  # 50th percentile (median)
user_auth_duration_seconds{quantile="0.95"}  # 95th percentile
user_auth_duration_seconds{quantile="0.99"}  # 99th percentile

# Average authentication time
rate(user_auth_duration_seconds_sum[1m]) / rate(user_auth_duration_seconds_count[1m])

# Login attempts by authentication method
sum by (auth_method) (rate(user_logins_total[1m])) * 60
```

### User Activity Metrics

```promql
# Current active users
user_active_count

# Current online users
user_online_count

# User engagement rate (engagement events per minute)
rate(user_engagement_total[1m]) * 60

# Average user activity score
rate(user_activity_score_sum[1m]) / rate(user_activity_score_count[1m])

# User activity score distribution
histogram_quantile(0.90, rate(user_activity_score_bucket[1m]))
```

### User Session Metrics

```promql
# Session creation rate (sessions per minute)
rate(user_sessions_total[1m]) * 60

# Average session duration (in minutes)
(rate(user_session_duration_sum[1m]) / rate(user_session_duration_count[1m])) / 60

# Total session time (in hours)
user_session_duration_total / 3600

# Session duration percentiles (in minutes)
histogram_quantile(0.50, rate(user_session_duration_bucket[1m])) / 60
histogram_quantile(0.95, rate(user_session_duration_bucket[1m])) / 60
```

### User Regional Activity

```promql
# User requests by region (requests per minute)
sum by (region) (rate(user_requests_by_region_total[1m])) * 60

# Top regions by user activity
topk(3, sum by (region) (rate(user_requests_by_region_total[1m])))

# Regional distribution percentage
(rate(user_requests_by_region_total[1m]) / ignoring(region) group_left sum(rate(user_requests_by_region_total[1m]))) * 100
```

### User System Metrics

```promql
# User cache efficiency
user_cache_size

# User request processing time percentiles
user_request_duration_seconds{quantile="0.95"}
user_request_duration_seconds{quantile="0.99"}

# User endpoint performance
sum by (endpoint) (rate(user_endpoint_duration_seconds_count[1m]))
avg by (endpoint) (rate(user_endpoint_duration_seconds_sum[1m]) / rate(user_endpoint_duration_seconds_count[1m]))
```

---

## Commerce Analytics Service Queries

### Order Metrics

```promql
# Order rate (orders per minute)
rate(commerce_orders_total[1m]) * 60

# Total orders processed
commerce_orders_total

# Orders by type
sum by (order_type) (rate(commerce_orders_total[1m])) * 60

# Orders by fulfillment type
sum by (fulfillment_type) (rate(commerce_orders_total[1m])) * 60

# Order processing time percentiles
commerce_order_processing_duration_seconds{quantile="0.50"}
commerce_order_processing_duration_seconds{quantile="0.95"}
commerce_order_processing_duration_seconds{quantile="0.99"}

# Average order processing time
rate(commerce_order_processing_duration_seconds_sum[1m]) / rate(commerce_order_processing_duration_seconds_count[1m])

# Currently active orders
commerce_orders_active
```

### Payment Metrics

```promql
# Payment rate (payments per minute)
rate(commerce_payments_total[1m]) * 60

# Payment success rate
(rate(commerce_payments_total{status="success"}[1m]) / rate(commerce_payments_total[1m])) * 100

# Payment failure rate
(rate(commerce_payments_total{status="failed"}[1m]) / rate(commerce_payments_total[1m])) * 100

# Payments by method
sum by (payment_method) (rate(commerce_payments_total[1m])) * 60

# Payments by currency
sum by (currency) (rate(commerce_payments_total[1m])) * 60

# Payment processing time percentiles
commerce_payment_processing_duration_seconds{quantile="0.50"}
commerce_payment_processing_duration_seconds{quantile="0.95"}
commerce_payment_processing_duration_seconds{quantile="0.99"}

# Average payment processing time
rate(commerce_payment_processing_duration_seconds_sum[1m]) / rate(commerce_payment_processing_duration_seconds_count[1m])
```

### Product & Inventory Metrics

```promql
# Product view rate (views per minute)
rate(commerce_products_views_total[1m]) * 60

# Product views by category
sum by (category) (rate(commerce_products_views_total[1m])) * 60

# Product views by device
sum by (device) (rate(commerce_products_views_total[1m])) * 60

# Current inventory levels
commerce_inventory_levels

# Inventory check latency
commerce_inventory_check_duration_seconds{quantile="0.95"}

# Average product rating
rate(commerce_product_rating_sum[1m]) / rate(commerce_product_rating_count[1m])

# Product rating distribution
histogram_quantile(0.90, rate(commerce_product_rating_bucket[1m]))
```

### Revenue & Financial Metrics

```promql
# Revenue rate (dollars per minute)
rate(commerce_revenue_total[1m]) / 100 * 60

# Total revenue (in dollars)
commerce_revenue_total / 100

# Average order value (in dollars)
rate(commerce_order_value_sum[1m]) / rate(commerce_order_value_count[1m])

# Order value percentiles
histogram_quantile(0.50, rate(commerce_order_value_bucket[1m]))  # Median order value
histogram_quantile(0.95, rate(commerce_order_value_bucket[1m]))  # 95th percentile

# Average shipping cost
rate(commerce_shipping_cost_sum[1m]) / rate(commerce_shipping_cost_count[1m])

# Total daily revenue estimate (in dollars)
increase(commerce_revenue_total[1d]) / 100
```

### Shopping Cart Metrics

```promql
# Cart action rate (actions per minute)
rate(commerce_cart_actions_total[1m]) * 60

# Cart actions by type
sum by (action) (rate(commerce_cart_actions_total[1m])) * 60

# Cart abandonment rate (if abandon action exists)
rate(commerce_cart_actions_total{action="abandon"}[1m]) / rate(commerce_cart_actions_total{action="add_item"}[1m]) * 100

# Cart conversion rate (checkout vs add_item)
rate(commerce_cart_actions_total{action="checkout"}[1m]) / rate(commerce_cart_actions_total{action="add_item"}[1m]) * 100
```

### Commerce Regional Activity

```promql
# Commerce requests by region (requests per minute)
sum by (region) (rate(commerce_requests_by_region_total[1m])) * 60

# Top regions by commerce activity
topk(3, sum by (region) (rate(commerce_requests_by_region_total[1m])))

# Regional commerce distribution percentage
(rate(commerce_requests_by_region_total[1m]) / ignoring(region) group_left sum(rate(commerce_requests_by_region_total[1m]))) * 100
```

### Commerce System Metrics

```promql
# Database connection pool usage
commerce_database_connections_active

# Database query latency percentiles
commerce_database_query_duration_seconds{quantile="0.95"}
commerce_database_query_duration_seconds{quantile="0.99"}

# Commerce endpoint performance
sum by (endpoint) (rate(commerce_endpoint_duration_seconds_count[1m]))
avg by (endpoint) (rate(commerce_endpoint_duration_seconds_sum[1m]) / rate(commerce_endpoint_duration_seconds_count[1m]))
```

---

## Cross-Service Queries

### Service Comparison

```promql
# Total requests across both services (per minute)
sum(rate(http_server_requests_total[1m])) * 60

# Request rate by service
sum by (service) (rate(http_server_requests_total[1m])) * 60

# Request latency comparison by service
avg by (service) (rate(http_server_requests_seconds_sum[1m]) / rate(http_server_requests_seconds_count[1m]))

# Error rate by service
sum by (service) (rate(http_server_requests_total{status=~"4..|5.."}[1m])) / sum by (service) (rate(http_server_requests_total[1m])) * 100
```

### Resource Usage Comparison

```promql
# Memory usage by service (in MB)
sum by (service) (jvm_memory_used_bytes{area="heap"}) / 1024 / 1024

# CPU usage by service
sum by (service) (rate(process_cpu_seconds_total[1m]))

# Garbage collection impact by service
sum by (service) (rate(jvm_gc_pause_seconds_sum[1m]))

# Thread count by service
sum by (service) (jvm_threads_live_threads)
```

### Business KPIs Across Services

```promql
# Overall user engagement vs commerce activity correlation
user_engagement_total and commerce_orders_total

# Revenue per active user (in dollars)
commerce_revenue_total / 100 / user_active_count

# User activity vs order correlation
rate(user_engagement_total[1m]) and rate(commerce_orders_total[1m])
```

---

## Alerting Queries

### User Analytics Alerts

```promql
# High authentication failure rate (>20%)
(rate(user_logins_total{success="false"}[5m]) / rate(user_logins_total[5m])) * 100 > 20

# Low active users (< 50)
user_active_count < 50

# High authentication latency (>2 seconds for 95th percentile)
user_auth_duration_seconds{quantile="0.95"} > 2

# No user registrations in last 10 minutes
increase(user_registrations_total[10m]) == 0

# User service down
up{job="user-analytics-service"} == 0
```

### Commerce Analytics Alerts

```promql
# High payment failure rate (>15%)
(rate(commerce_payments_total{status="failed"}[5m]) / rate(commerce_payments_total[5m])) * 100 > 15

# Low inventory levels (< 100)
commerce_inventory_levels < 100

# High order processing latency (>5 seconds for 95th percentile)
commerce_order_processing_duration_seconds{quantile="0.95"} > 5

# No orders in last 15 minutes
increase(commerce_orders_total[15m]) == 0

# High database connection usage (>40 connections)
commerce_database_connections_active > 40

# Commerce service down
up{job="commerce-analytics-service"} == 0
```

### System-Level Alerts

```promql
# High memory usage (>80% of heap)
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100 > 80

# High GC pressure (>100ms GC time per second)
rate(jvm_gc_pause_seconds_sum[1m]) > 0.1

# High error rate (>5% of requests)
sum(rate(http_server_requests_total{status=~"4..|5.."}[5m])) / sum(rate(http_server_requests_total[5m])) * 100 > 5
```

---

## Dashboard Queries

### User Analytics Dashboard

```promql
# User Growth Panel
increase(user_registrations_total[1h])  # Hourly registrations
increase(user_registrations_total[1d])  # Daily registrations

# User Activity Panel
user_active_count                       # Current active users
user_online_count                       # Current online users
rate(user_engagement_total[1m]) * 60    # Engagement events per minute

# Authentication Performance Panel
user_auth_duration_seconds{quantile="0.50"}  # Median auth time
user_auth_duration_seconds{quantile="0.95"}  # 95th percentile auth time
(rate(user_logins_total{success="true"}[1m]) / rate(user_logins_total[1m])) * 100  # Success rate

# Regional Distribution Panel
sum by (region) (rate(user_requests_by_region_total[1m])) * 60

# Session Analytics Panel
rate(user_sessions_total[1m]) * 60                    # Session creation rate
rate(user_session_duration_sum[1m]) / rate(user_session_duration_count[1m]) / 60  # Avg session duration (minutes)
```

### Commerce Analytics Dashboard

```promql
# Sales Overview Panel
rate(commerce_orders_total[1m]) * 60                  # Orders per minute
commerce_revenue_total / 100                          # Total revenue (dollars)
rate(commerce_revenue_total[1m]) / 100 * 60           # Revenue rate (dollars per minute)

# Order Performance Panel
commerce_order_processing_duration_seconds{quantile="0.50"}  # Median processing time
commerce_order_processing_duration_seconds{quantile="0.95"}  # 95th percentile
commerce_orders_active                                       # Active orders

# Payment Performance Panel
rate(commerce_payments_total[1m]) * 60                                        # Payments per minute
(rate(commerce_payments_total{status="success"}[1m]) / rate(commerce_payments_total[1m])) * 100  # Success rate
commerce_payment_processing_duration_seconds{quantile="0.95"}                 # Payment latency

# Inventory Panel
commerce_inventory_levels                              # Current inventory
rate(commerce_products_views_total[1m]) * 60          # Product views per minute
rate(commerce_product_rating_sum[1m]) / rate(commerce_product_rating_count[1m])  # Average rating

# Cart Analytics Panel
rate(commerce_cart_actions_total[1m]) * 60            # Cart actions per minute
sum by (action) (rate(commerce_cart_actions_total[1m])) * 60  # Actions by type
```

### Business Intelligence Queries

```promql
# Revenue Insights
# Average order value trend
rate(commerce_order_value_sum[1m]) / rate(commerce_order_value_count[1m])

# Revenue by payment method
sum by (payment_method) (rate(commerce_orders_total[1m])) * 60

# Daily revenue growth
(increase(commerce_revenue_total[1d]) - increase(commerce_revenue_total[1d] offset 1d)) / increase(commerce_revenue_total[1d] offset 1d) * 100

# Customer Insights
# User lifetime value estimate (revenue per user)
commerce_revenue_total / 100 / user_registrations_total

# User engagement correlation with orders
rate(user_engagement_total[1m]) and rate(commerce_orders_total[1m])

# Conversion funnel
# Product views to cart additions
rate(commerce_cart_actions_total{action="add_item"}[1m]) / rate(commerce_products_views_total[1m]) * 100

# Cart additions to orders
rate(commerce_orders_total[1m]) / rate(commerce_cart_actions_total{action="add_item"}[1m]) * 100
```

---

## Advanced Analytics Queries

### Performance Analysis

```promql
# Service response time comparison
avg by (service) (rate(http_server_requests_seconds_sum{uri!="/actuator/prometheus"}[1m]) / rate(http_server_requests_seconds_count{uri!="/actuator/prometheus"}[1m]))

# Request volume by endpoint
sum by (uri, service) (rate(http_server_requests_total[1m])) * 60

# Slowest endpoints (95th percentile response time)
topk(5, http_server_requests_seconds{quantile="0.95"})
```

### Capacity Planning

```promql
# Request rate growth (comparing current hour to previous hour)
(sum(rate(http_server_requests_total[1h])) - sum(rate(http_server_requests_total[1h] offset 1h))) / sum(rate(http_server_requests_total[1h] offset 1h)) * 100

# Peak vs average load ratio
max_over_time(sum(rate(http_server_requests_total[1m]))[1h]) / avg_over_time(sum(rate(http_server_requests_total[1m]))[1h])

# Resource utilization trends
increase(jvm_memory_used_bytes{area="heap"}[1h])
increase(process_cpu_seconds_total[1h])
```

### Error Analysis

```promql
# Error rate by status code
sum by (status, service) (rate(http_server_requests_total{status=~"4..|5.."}[1m])) * 60

# Most common errors
topk(5, sum by (status, uri) (rate(http_server_requests_total{status=~"4..|5.."}[1m])))

# Error spike detection (current rate vs 1 hour ago)
sum(rate(http_server_requests_total{status=~"4..|5.."}[5m])) / sum(rate(http_server_requests_total{status=~"4..|5.."}[5m] offset 1h))
```

---

## Sample Grafana Panel Queries

### Single Stat Panels

```promql
# Current Active Users
user_active_count

# Current Revenue (in dollars)
commerce_revenue_total / 100

# Orders per minute
rate(commerce_orders_total[1m]) * 60

# Authentication success rate
(rate(user_logins_total{success="true"}[1m]) / rate(user_logins_total[1m])) * 100
```

### Time Series Panels

```promql
# User registrations over time
increase(user_registrations_total[5m])

# Revenue over time (dollars per 5 minutes)
increase(commerce_revenue_total[5m]) / 100

# Request rates by service
sum by (service) (rate(http_server_requests_total[1m])) * 60

# Response time percentiles
http_server_requests_seconds{quantile="0.50"}
http_server_requests_seconds{quantile="0.95"}
http_server_requests_seconds{quantile="0.99"}
```

### Heatmap Panels

```promql
# Order value distribution
sum by (le) (rate(commerce_order_value_bucket[1m]))

# Session duration distribution
sum by (le) (rate(user_session_duration_bucket[1m]))

# Response time distribution
sum by (le, service) (rate(http_server_requests_seconds_bucket[1m]))
```

---

## Testing Your Queries

To test these queries in Prometheus:

1. **Open Prometheus UI**: http://localhost:9090
2. **Go to Graph tab**: Click on "Graph" in the top navigation
3. **Enter any query**: Copy and paste queries from above
4. **Execute**: Click "Execute" or press Enter
5. **View results**: Switch between "Table" and "Graph" views

### Quick Test Queries

Start with these simple queries to verify data is flowing:

```promql
# Verify user metrics are available
user_active_count

# Verify commerce metrics are available  
commerce_inventory_levels

# Verify both services are up
up

# Show all user metrics
{__name__=~"user_.*"}

# Show all commerce metrics
{__name__=~"commerce_.*"}
```

---

## Common PromQL Functions Used

- `rate()` - Calculate per-second rate over time range
- `increase()` - Calculate increase over time range  
- `sum()` - Sum values across dimensions
- `avg()` - Average values across dimensions
- `max()` - Maximum values across dimensions
- `min()` - Minimum values across dimensions
- `topk()` - Top K values
- `histogram_quantile()` - Calculate quantiles from histograms
- `by()` - Group by specific labels
- `without()` - Group by all labels except specified ones
- `ignoring()` - Ignore specific labels when matching
- `offset` - Look at data from a previous time period

## Time Range Examples

- `[1m]` - Last 1 minute
- `[5m]` - Last 5 minutes  
- `[1h]` - Last 1 hour
- `[1d]` - Last 1 day
- `[1w]` - Last 1 week

Remember to adjust time ranges based on your metric collection frequency and analysis needs!
