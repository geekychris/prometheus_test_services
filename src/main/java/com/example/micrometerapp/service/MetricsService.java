package com.example.micrometerapp.service;

import com.github.javafaker.Faker;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    private final Faker faker = new Faker();
    
    private final MeterRegistry meterRegistry;
    
    // Counters
    private final Counter requestCounter;
    private final Counter errorCounter;
    private final Counter orderCounter;
    private final Counter userRegistrationCounter;
    
    // Gauges
    private final AtomicInteger activeUsers;
    private final AtomicInteger queueSize;
    private final AtomicLong memoryUsage;
    private final AtomicInteger connectionPoolSize;
    
    // Timers
    private final Timer requestTimer;
    private final Timer databaseQueryTimer;
    private final Timer paymentProcessingTimer;
    
    // Distribution Summary (Histogram)
    private final DistributionSummary orderValueSummary;
    private final DistributionSummary messageSizeSummary;
    
    // Custom metrics with tags
    private final Map<String, Counter> regionCounters = new HashMap<>();
    private final Map<String, Timer> endpointTimers = new HashMap<>();

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.requestCounter = Counter.builder("app.requests.total")
                .description("Total number of requests")
                .register(meterRegistry);
        
        this.errorCounter = Counter.builder("app.errors.total")
                .description("Total number of errors")
                .register(meterRegistry);
        
        this.orderCounter = Counter.builder("app.orders.total")
                .description("Total number of orders")
                .register(meterRegistry);
        
        this.userRegistrationCounter = Counter.builder("app.users.registrations.total")
                .description("Total number of user registrations")
                .register(meterRegistry);
        
        // Initialize atomic values for gauges
        this.activeUsers = new AtomicInteger(0);
        this.queueSize = new AtomicInteger(0);
        this.memoryUsage = new AtomicLong(0);
        this.connectionPoolSize = new AtomicInteger(10);
        
        // Register gauges
        Gauge.builder("app.users.active", activeUsers, AtomicInteger::doubleValue)
                .description("Number of currently active users")
                .register(meterRegistry);
        
        Gauge.builder("app.queue.size", queueSize, AtomicInteger::doubleValue)
                .description("Current queue size")
                .register(meterRegistry);
        
        Gauge.builder("app.memory.usage.bytes", memoryUsage, AtomicLong::doubleValue)
                .description("Current memory usage in bytes")
                .register(meterRegistry);
        
        Gauge.builder("app.database.connections.active", connectionPoolSize, AtomicInteger::doubleValue)
                .description("Number of active database connections")
                .register(meterRegistry);
        
        // Initialize timers
        this.requestTimer = Timer.builder("app.request.duration")
                .description("Request processing time")
                .register(meterRegistry);
        
        this.databaseQueryTimer = Timer.builder("app.database.query.duration")
                .description("Database query execution time")
                .register(meterRegistry);
        
        this.paymentProcessingTimer = Timer.builder("app.payment.processing.duration")
                .description("Payment processing time")
                .register(meterRegistry);
        
        // Initialize distribution summaries
        this.orderValueSummary = DistributionSummary.builder("app.order.value")
                .description("Distribution of order values")
                .baseUnit("dollars")
                .register(meterRegistry);
        
        this.messageSizeSummary = DistributionSummary.builder("app.message.size")
                .description("Distribution of message sizes")
                .baseUnit("bytes")
                .register(meterRegistry);
        
        // Initialize regional counters
        initializeRegionalCounters();
        initializeEndpointTimers();
    }
    
    private void initializeRegionalCounters() {
        String[] regions = {"us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1"};
        for (String region : regions) {
            regionCounters.put(region, Counter.builder("app.requests.by.region")
                    .tag("region", region)
                    .description("Requests by region")
                    .register(meterRegistry));
        }
    }
    
    private void initializeEndpointTimers() {
        String[] endpoints = {"/api/users", "/api/orders", "/api/products", "/api/payments"};
        for (String endpoint : endpoints) {
            endpointTimers.put(endpoint, Timer.builder("app.endpoint.duration")
                    .tag("endpoint", endpoint)
                    .description("Endpoint processing time")
                    .register(meterRegistry));
        }
    }
    
    public void simulateUserActivity() {
        // Simulate request activity
        requestCounter.increment();
        
        // Randomly generate errors (5% chance)
        if (ThreadLocalRandom.current().nextDouble() < 0.05) {
            Counter.builder("app.errors.total")
                .description("Total number of errors")
                .tag("error_type", getRandomErrorType())
                .tag("status_code", getRandomErrorCode())
                .register(meterRegistry)
                .increment();
        }
        
        // Update active users gauge
        int change = ThreadLocalRandom.current().nextInt(-5, 6);
        int newValue = Math.max(0, Math.min(200, activeUsers.get() + change));
        activeUsers.set(newValue);
        
        // Update queue size
        queueSize.set(ThreadLocalRandom.current().nextInt(0, 50));
        
        // Update memory usage (simulate in MB converted to bytes)
        long memoryMB = ThreadLocalRandom.current().nextLong(100, 1000);
        memoryUsage.set(memoryMB * 1024 * 1024);
        
        // Update connection pool
        connectionPoolSize.set(ThreadLocalRandom.current().nextInt(5, 20));
        
        logger.debug("Updated user activity metrics");
    }
    
    public void simulateOrderProcessing() {
        // Record order
        Counter.builder("app.orders.total")
            .description("Total number of orders")
            .tag("order_type", getRandomOrderType())
            .tag("payment_method", getRandomPaymentMethod())
            .register(meterRegistry)
            .increment();
        
        // Record order value
        double orderValue = ThreadLocalRandom.current().nextDouble(10.0, 500.0);
        orderValueSummary.record(orderValue);
        
        // Simulate payment processing time
        Duration paymentDuration = Duration.ofMillis(ThreadLocalRandom.current().nextLong(100, 2000));
        paymentProcessingTimer.record(paymentDuration);
        
        logger.debug("Processed order: ${} with payment time: {}ms", orderValue, paymentDuration.toMillis());
    }
    
    public void simulateDatabaseActivity() {
        // Simulate database query
        Duration queryDuration = Duration.ofMillis(ThreadLocalRandom.current().nextLong(5, 500));
        databaseQueryTimer.record(queryDuration);
        
        // Simulate message processing
        int messageSize = ThreadLocalRandom.current().nextInt(100, 10000);
        messageSizeSummary.record(messageSize);
        
        logger.debug("Database query took {}ms, message size: {} bytes", queryDuration.toMillis(), messageSize);
    }
    
    public void simulateRegionalActivity() {
        // Randomly select a region and increment its counter
        String[] regions = {"us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1"};
        String randomRegion = regions[ThreadLocalRandom.current().nextInt(regions.length)];
        regionCounters.get(randomRegion).increment();
    }
    
    public void simulateEndpointActivity() {
        // Randomly select an endpoint and record its processing time
        String[] endpoints = {"/api/users", "/api/orders", "/api/products", "/api/payments"};
        String randomEndpoint = endpoints[ThreadLocalRandom.current().nextInt(endpoints.length)];
        Duration endpointDuration = Duration.ofMillis(ThreadLocalRandom.current().nextLong(50, 1000));
        endpointTimers.get(randomEndpoint).record(endpointDuration);
    }
    
    public void recordUserRegistration() {
        Counter.builder("app.users.registrations.total")
            .description("Total number of user registrations")
            .tag("source", getRandomRegistrationSource())
            .tag("user_type", getRandomUserType())
            .register(meterRegistry)
            .increment();
    }
    
    public void recordApiRequest(String endpoint, Duration duration) {
        requestTimer.record(duration);
        
        Timer endpointTimer = endpointTimers.get(endpoint);
        if (endpointTimer != null) {
            endpointTimer.record(duration);
        }
    }
    
    private String getRandomErrorType() {
        String[] errorTypes = {"validation", "authentication", "authorization", "database", "network", "timeout"};
        return errorTypes[ThreadLocalRandom.current().nextInt(errorTypes.length)];
    }
    
    private String getRandomErrorCode() {
        String[] codes = {"400", "401", "403", "404", "500", "502", "503"};
        return codes[ThreadLocalRandom.current().nextInt(codes.length)];
    }
    
    private String getRandomOrderType() {
        String[] types = {"standard", "express", "overnight", "international"};
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }
    
    private String getRandomPaymentMethod() {
        String[] methods = {"credit_card", "debit_card", "paypal", "apple_pay", "google_pay", "bank_transfer"};
        return methods[ThreadLocalRandom.current().nextInt(methods.length)];
    }
    
    private String getRandomRegistrationSource() {
        String[] sources = {"web", "mobile_app", "social_login", "referral"};
        return sources[ThreadLocalRandom.current().nextInt(sources.length)];
    }
    
    private String getRandomUserType() {
        String[] types = {"free", "premium", "enterprise"};
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }
}
