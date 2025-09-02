package com.example.commerceanalytics.service;

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
public class CommerceMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(CommerceMetricsService.class);
    private final Faker faker = new Faker();
    
    private final MeterRegistry meterRegistry;
    
    // Commerce-specific Counters
    private final Counter orderCounter;
    private final Counter paymentCounter;
    private final Counter productViewCounter;
    private final Counter cartActionCounter;
    
    // Commerce-specific Gauges
    private final AtomicInteger inventoryLevels;
    private final AtomicInteger activeOrders;
    private final AtomicLong totalRevenue;
    private final AtomicInteger databaseConnections;
    
    // Commerce-specific Timers
    private final Timer orderProcessingTimer;
    private final Timer paymentProcessingTimer;
    private final Timer inventoryCheckTimer;
    private final Timer databaseQueryTimer;
    
    // Distribution Summary for commerce metrics
    private final DistributionSummary orderValueSummary;
    private final DistributionSummary shippingCostSummary;
    private final DistributionSummary productRatingSummary;
    
    // Regional commerce metrics
    private final Map<String, Counter> commerceRegionCounters = new HashMap<>();
    private final Map<String, Timer> commerceEndpointTimers = new HashMap<>();

    public CommerceMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize commerce-specific counters
        this.orderCounter = Counter.builder("commerce.orders.total")
                .description("Total number of orders")
                .register(meterRegistry);
        
        this.paymentCounter = Counter.builder("commerce.payments.total")
                .description("Total number of payments")
                .register(meterRegistry);
        
        this.productViewCounter = Counter.builder("commerce.products.views.total")
                .description("Total number of product views")
                .register(meterRegistry);
        
        this.cartActionCounter = Counter.builder("commerce.cart.actions.total")
                .description("Total number of cart actions")
                .register(meterRegistry);
        
        // Initialize atomic values for commerce gauges
        this.inventoryLevels = new AtomicInteger(1000);
        this.activeOrders = new AtomicInteger(0);
        this.totalRevenue = new AtomicLong(0);
        this.databaseConnections = new AtomicInteger(10);
        
        // Register commerce gauges
        Gauge.builder("commerce.inventory.levels", inventoryLevels, AtomicInteger::doubleValue)
                .description("Current inventory levels")
                .register(meterRegistry);
        
        Gauge.builder("commerce.orders.active", activeOrders, AtomicInteger::doubleValue)
                .description("Number of currently active orders")
                .register(meterRegistry);
        
        Gauge.builder("commerce.revenue.total", totalRevenue, AtomicLong::doubleValue)
                .description("Total revenue in cents")
                .register(meterRegistry);
        
        Gauge.builder("commerce.database.connections.active", databaseConnections, AtomicInteger::doubleValue)
                .description("Number of active database connections")
                .register(meterRegistry);
        
        // Initialize commerce timers
        this.orderProcessingTimer = Timer.builder("commerce.order.processing.duration")
                .description("Order processing time")
                .register(meterRegistry);
        
        this.paymentProcessingTimer = Timer.builder("commerce.payment.processing.duration")
                .description("Payment processing time")
                .register(meterRegistry);
        
        this.inventoryCheckTimer = Timer.builder("commerce.inventory.check.duration")
                .description("Inventory check processing time")
                .register(meterRegistry);
        
        this.databaseQueryTimer = Timer.builder("commerce.database.query.duration")
                .description("Database query execution time")
                .register(meterRegistry);
        
        // Initialize commerce distribution summaries
        this.orderValueSummary = DistributionSummary.builder("commerce.order.value")
                .description("Distribution of order values")
                .baseUnit("dollars")
                .register(meterRegistry);
        
        this.shippingCostSummary = DistributionSummary.builder("commerce.shipping.cost")
                .description("Distribution of shipping costs")
                .baseUnit("dollars")
                .register(meterRegistry);
        
        this.productRatingSummary = DistributionSummary.builder("commerce.product.rating")
                .description("Distribution of product ratings")
                .register(meterRegistry);
        
        // Initialize regional commerce counters
        initializeCommerceRegionalCounters();
        initializeCommerceEndpointTimers();
    }
    
    private void initializeCommerceRegionalCounters() {
        String[] regions = {"us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1", "ca-central-1"};
        for (String region : regions) {
            commerceRegionCounters.put(region, Counter.builder("commerce.requests.by.region")
                    .tag("region", region)
                    .description("Commerce requests by region")
                    .register(meterRegistry));
        }
    }
    
    private void initializeCommerceEndpointTimers() {
        String[] endpoints = {"/api/orders", "/api/payments", "/api/products", "/api/cart"};
        for (String endpoint : endpoints) {
            commerceEndpointTimers.put(endpoint, Timer.builder("commerce.endpoint.duration")
                    .tag("endpoint", endpoint)
                    .description("Commerce endpoint processing time")
                    .register(meterRegistry));
        }
    }
    
    public void simulateOrderProcessing() {
        // Record order
        Counter.builder("commerce.orders.total")
            .description("Total number of orders")
            .tag("order_type", getRandomOrderType())
            .tag("payment_method", getRandomPaymentMethod())
            .tag("fulfillment_type", getRandomFulfillmentType())
            .register(meterRegistry)
            .increment();
        
        // Record order value
        double orderValue = ThreadLocalRandom.current().nextDouble(10.0, 500.0);
        orderValueSummary.record(orderValue);
        totalRevenue.addAndGet((long)(orderValue * 100)); // Store in cents
        
        // Record shipping cost
        double shippingCost = ThreadLocalRandom.current().nextDouble(0.0, 25.0);
        shippingCostSummary.record(shippingCost);
        
        // Simulate order processing time
        Duration processingDuration = Duration.ofMillis(ThreadLocalRandom.current().nextLong(200, 3000));
        orderProcessingTimer.record(processingDuration);
        
        // Update active orders
        int orderChange = ThreadLocalRandom.current().nextInt(-2, 5);
        int newActiveOrders = Math.max(0, Math.min(100, activeOrders.get() + orderChange));
        activeOrders.set(newActiveOrders);
        
        logger.debug("Processed order: ${} with processing time: {}ms", orderValue, processingDuration.toMillis());
    }
    
    public void simulatePaymentProcessing() {
        // Record payment
        Counter.builder("commerce.payments.total")
            .description("Total number of payments")
            .tag("payment_method", getRandomPaymentMethod())
            .tag("currency", getRandomCurrency())
            .tag("status", getRandomPaymentStatus())
            .register(meterRegistry)
            .increment();
        
        // Simulate payment processing time
        Duration paymentDuration = Duration.ofMillis(ThreadLocalRandom.current().nextLong(300, 2500));
        paymentProcessingTimer.record(paymentDuration);
        
        logger.debug("Payment processed in {}ms", paymentDuration.toMillis());
    }
    
    public void simulateProductActivity() {
        // Record product view
        Counter.builder("commerce.products.views.total")
            .description("Total number of product views")
            .tag("category", getRandomProductCategory())
            .tag("device", getRandomDevice())
            .register(meterRegistry)
            .increment();
        
        // Record product rating
        double rating = ThreadLocalRandom.current().nextDouble(1.0, 5.0);
        productRatingSummary.record(rating);
        
        // Update inventory levels
        int inventoryChange = ThreadLocalRandom.current().nextInt(-10, 5);
        int newInventory = Math.max(0, Math.min(5000, inventoryLevels.get() + inventoryChange));
        inventoryLevels.set(newInventory);
        
        // Simulate inventory check time
        Duration inventoryDuration = Duration.ofMillis(ThreadLocalRandom.current().nextLong(25, 200));
        inventoryCheckTimer.record(inventoryDuration);
    }
    
    public void simulateCartActivity() {
        // Record cart action
        Counter.builder("commerce.cart.actions.total")
            .description("Total number of cart actions")
            .tag("action", getRandomCartAction())
            .tag("device", getRandomDevice())
            .register(meterRegistry)
            .increment();
    }
    
    public void simulateDatabaseActivity() {
        // Simulate database query
        Duration queryDuration = Duration.ofMillis(ThreadLocalRandom.current().nextLong(10, 800));
        databaseQueryTimer.record(queryDuration);
        
        // Update database connections
        int connectionChange = ThreadLocalRandom.current().nextInt(-2, 3);
        int newConnections = Math.max(1, Math.min(50, databaseConnections.get() + connectionChange));
        databaseConnections.set(newConnections);
        
        logger.debug("Database query took {}ms, active connections: {}", queryDuration.toMillis(), newConnections);
    }
    
    public void simulateCommerceRegionalActivity() {
        // Randomly select a region and increment its counter
        String[] regions = {"us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1", "ca-central-1"};
        String randomRegion = regions[ThreadLocalRandom.current().nextInt(regions.length)];
        commerceRegionCounters.get(randomRegion).increment();
    }
    
    public void simulateCommerceEndpointActivity() {
        // Randomly select a commerce endpoint and record its processing time
        String[] endpoints = {"/api/orders", "/api/payments", "/api/products", "/api/cart"};
        String randomEndpoint = endpoints[ThreadLocalRandom.current().nextInt(endpoints.length)];
        Duration endpointDuration = Duration.ofMillis(ThreadLocalRandom.current().nextLong(100, 1500));
        commerceEndpointTimers.get(randomEndpoint).record(endpointDuration);
    }
    
    public void recordCommerceApiRequest(String endpoint, Duration duration) {
        Timer endpointTimer = commerceEndpointTimers.get(endpoint);
        if (endpointTimer != null) {
            endpointTimer.record(duration);
        }
    }
    
    private String getRandomOrderType() {
        String[] types = {"standard", "express", "overnight", "international", "subscription"};
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }
    
    private String getRandomPaymentMethod() {
        String[] methods = {"credit_card", "debit_card", "paypal", "apple_pay", "google_pay", "bank_transfer", "crypto"};
        return methods[ThreadLocalRandom.current().nextInt(methods.length)];
    }
    
    private String getRandomFulfillmentType() {
        String[] types = {"warehouse", "dropship", "digital", "pickup"};
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }
    
    private String getRandomCurrency() {
        String[] currencies = {"USD", "EUR", "GBP", "CAD", "JPY", "AUD"};
        return currencies[ThreadLocalRandom.current().nextInt(currencies.length)];
    }
    
    private String getRandomPaymentStatus() {
        String[] statuses = {"success", "failed", "pending", "cancelled"};
        return statuses[ThreadLocalRandom.current().nextInt(statuses.length)];
    }
    
    private String getRandomProductCategory() {
        String[] categories = {"electronics", "clothing", "books", "home", "sports", "automotive", "beauty"};
        return categories[ThreadLocalRandom.current().nextInt(categories.length)];
    }
    
    private String getRandomDevice() {
        String[] devices = {"desktop", "mobile", "tablet", "api"};
        return devices[ThreadLocalRandom.current().nextInt(devices.length)];
    }
    
    private String getRandomCartAction() {
        String[] actions = {"add_item", "remove_item", "update_quantity", "apply_coupon", "checkout", "abandon"};
        return actions[ThreadLocalRandom.current().nextInt(actions.length)];
    }
}
