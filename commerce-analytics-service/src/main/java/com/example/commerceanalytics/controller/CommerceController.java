package com.example.commerceanalytics.controller;

import com.example.commerceanalytics.service.CommerceMetricsService;
import com.github.javafaker.Faker;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api")
public class CommerceController {

    private static final Logger logger = LoggerFactory.getLogger(CommerceController.class);
    private final CommerceMetricsService commerceMetricsService;
    private final Faker faker = new Faker();

    public CommerceController(CommerceMetricsService commerceMetricsService) {
        this.commerceMetricsService = commerceMetricsService;
    }

    @GetMapping("/orders")
    @Timed(value = "api.orders.get", description = "Get orders endpoint timing")
    public ResponseEntity<Map<String, Object>> getOrders() {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(75, 400);
        
        commerceMetricsService.simulateOrderProcessing();
        commerceMetricsService.simulateDatabaseActivity();
        commerceMetricsService.simulateCommerceRegionalActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("orders", generateFakeOrders());
        response.put("total", ThreadLocalRandom.current().nextInt(50, 500));
        response.put("timestamp", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        commerceMetricsService.recordCommerceApiRequest("/api/orders", duration);
        
        logger.info("GET /api/orders processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders")
    @Timed(value = "api.orders.post", description = "Create order endpoint timing")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody(required = false) Map<String, Object> order) {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(200, 800);
        
        commerceMetricsService.simulateOrderProcessing();
        commerceMetricsService.simulateDatabaseActivity();
        commerceMetricsService.simulateProductActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", ThreadLocalRandom.current().nextLong(10000, 999999));
        response.put("total", ThreadLocalRandom.current().nextDouble(10.0, 500.0));
        response.put("status", "confirmed");
        response.put("created", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        commerceMetricsService.recordCommerceApiRequest("/api/orders", duration);
        
        logger.info("POST /api/orders processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products")
    @Timed(value = "api.products.get", description = "Get products endpoint timing")
    public ResponseEntity<Map<String, Object>> getProducts() {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(30, 200);
        
        commerceMetricsService.simulateProductActivity();
        commerceMetricsService.simulateDatabaseActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", generateFakeProducts());
        response.put("total", ThreadLocalRandom.current().nextInt(200, 2000));
        response.put("timestamp", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        commerceMetricsService.recordCommerceApiRequest("/api/products", duration);
        
        logger.info("GET /api/products processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments")
    @Timed(value = "api.payments.post", description = "Process payment endpoint timing")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody(required = false) Map<String, Object> payment) {
        Instant start = Instant.now();
        
        // Simulate longer processing for payments
        simulateDelay(500, 2000);
        
        commerceMetricsService.simulatePaymentProcessing();
        commerceMetricsService.simulateOrderProcessing();
        commerceMetricsService.simulateDatabaseActivity();
        
        // Simulate payment failure occasionally (10% chance)
        boolean paymentSuccess = ThreadLocalRandom.current().nextDouble() > 0.1;
        
        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", ThreadLocalRandom.current().nextLong(100000, 9999999));
        response.put("status", paymentSuccess ? "success" : "failed");
        response.put("amount", ThreadLocalRandom.current().nextDouble(10.0, 1000.0));
        response.put("processed", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        commerceMetricsService.recordCommerceApiRequest("/api/payments", duration);
        
        logger.info("POST /api/payments processed in {}ms with status: {}", 
                   duration.toMillis(), response.get("status"));
        
        if (paymentSuccess) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(402).body(response);
        }
    }

    @GetMapping("/cart")
    @Timed(value = "api.cart.get", description = "Get cart endpoint timing")
    public ResponseEntity<Map<String, Object>> getCart(@RequestParam(defaultValue = "1") Long userId) {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(50, 250);
        
        commerceMetricsService.simulateCartActivity();
        commerceMetricsService.simulateProductActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("items", generateFakeCartItems());
        response.put("totalValue", ThreadLocalRandom.current().nextDouble(25.0, 300.0));
        response.put("itemCount", ThreadLocalRandom.current().nextInt(1, 8));
        response.put("timestamp", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        commerceMetricsService.recordCommerceApiRequest("/api/cart", duration);
        
        logger.info("GET /api/cart processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cart")
    @Timed(value = "api.cart.post", description = "Update cart endpoint timing")
    public ResponseEntity<Map<String, Object>> updateCart(@RequestBody(required = false) Map<String, Object> cartUpdate) {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(75, 300);
        
        commerceMetricsService.simulateCartActivity();
        commerceMetricsService.simulateProductActivity();
        commerceMetricsService.simulateDatabaseActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("cartId", ThreadLocalRandom.current().nextLong(10000, 99999));
        response.put("action", getRandomCartAction());
        response.put("totalValue", ThreadLocalRandom.current().nextDouble(25.0, 300.0));
        response.put("updated", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        commerceMetricsService.recordCommerceApiRequest("/api/cart", duration);
        
        logger.info("POST /api/cart processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health-check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        commerceMetricsService.simulateProductActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "commerce-analytics");
        response.put("timestamp", Instant.now());
        response.put("uptime", "running");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/simulate/{type}")
    public ResponseEntity<Map<String, Object>> simulateActivity(@PathVariable String type) {
        Map<String, Object> response = new HashMap<>();
        
        switch (type.toLowerCase()) {
            case "orders":
                commerceMetricsService.simulateOrderProcessing();
                response.put("message", "Order processing simulated");
                break;
            case "payments":
                commerceMetricsService.simulatePaymentProcessing();
                response.put("message", "Payment processing simulated");
                break;
            case "products":
                commerceMetricsService.simulateProductActivity();
                response.put("message", "Product activity simulated");
                break;
            case "cart":
                commerceMetricsService.simulateCartActivity();
                response.put("message", "Cart activity simulated");
                break;
            case "database":
                commerceMetricsService.simulateDatabaseActivity();
                response.put("message", "Database activity simulated");
                break;
            case "regions":
                commerceMetricsService.simulateCommerceRegionalActivity();
                response.put("message", "Regional commerce activity simulated");
                break;
            case "endpoints":
                commerceMetricsService.simulateCommerceEndpointActivity();
                response.put("message", "Commerce endpoint activity simulated");
                break;
            case "all":
                commerceMetricsService.simulateOrderProcessing();
                commerceMetricsService.simulatePaymentProcessing();
                commerceMetricsService.simulateProductActivity();
                commerceMetricsService.simulateCartActivity();
                commerceMetricsService.simulateDatabaseActivity();
                commerceMetricsService.simulateCommerceRegionalActivity();
                commerceMetricsService.simulateCommerceEndpointActivity();
                response.put("message", "All commerce activities simulated");
                break;
            default:
                response.put("error", "Unknown simulation type");
                return ResponseEntity.badRequest().body(response);
        }
        
        response.put("type", type);
        response.put("service", "commerce-analytics");
        response.put("timestamp", Instant.now());
        
        logger.info("Simulated commerce activity: {}", type);
        return ResponseEntity.ok(response);
    }

    private void simulateDelay(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Object generateFakeOrders() {
        int count = ThreadLocalRandom.current().nextInt(3, 10);
        Map<String, Object>[] orders = new Map[count];
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> order = new HashMap<>();
            order.put("id", ThreadLocalRandom.current().nextLong(10000, 999999));
            order.put("customer", faker.name().fullName());
            order.put("product", faker.commerce().productName());
            order.put("total", ThreadLocalRandom.current().nextDouble(10.0, 500.0));
            order.put("status", getRandomOrderStatus());
            order.put("fulfillmentType", getRandomFulfillmentType());
            orders[i] = order;
        }
        
        return orders;
    }

    private Object generateFakeProducts() {
        int count = ThreadLocalRandom.current().nextInt(8, 20);
        Map<String, Object>[] products = new Map[count];
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> product = new HashMap<>();
            product.put("id", ThreadLocalRandom.current().nextLong(1, 1000));
            product.put("name", faker.commerce().productName());
            product.put("price", ThreadLocalRandom.current().nextDouble(5.0, 200.0));
            product.put("category", faker.commerce().department());
            product.put("inStock", ThreadLocalRandom.current().nextBoolean());
            product.put("rating", ThreadLocalRandom.current().nextDouble(1.0, 5.0));
            products[i] = product;
        }
        
        return products;
    }
    
    private Object generateFakeCartItems() {
        int count = ThreadLocalRandom.current().nextInt(1, 6);
        Map<String, Object>[] items = new Map[count];
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", ThreadLocalRandom.current().nextLong(1, 1000));
            item.put("name", faker.commerce().productName());
            item.put("price", ThreadLocalRandom.current().nextDouble(5.0, 100.0));
            item.put("quantity", ThreadLocalRandom.current().nextInt(1, 5));
            items[i] = item;
        }
        
        return items;
    }

    private String getRandomOrderStatus() {
        String[] statuses = {"pending", "processing", "shipped", "delivered", "cancelled"};
        return statuses[ThreadLocalRandom.current().nextInt(statuses.length)];
    }
    
    private String getRandomFulfillmentType() {
        String[] types = {"warehouse", "dropship", "digital", "pickup"};
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }
    
    private String getRandomCartAction() {
        String[] actions = {"add_item", "remove_item", "update_quantity", "apply_coupon", "checkout", "abandon"};
        return actions[ThreadLocalRandom.current().nextInt(actions.length)];
    }
}
