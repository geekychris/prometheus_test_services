package com.example.micrometerapp.controller;

import com.example.micrometerapp.service.MetricsService;
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
public class DemoController {

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);
    private final MetricsService metricsService;
    private final Faker faker = new Faker();

    public DemoController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/users")
    @Timed(value = "api.users.get", description = "Get users endpoint timing")
    public ResponseEntity<Map<String, Object>> getUsers() {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(50, 300);
        
        metricsService.simulateUserActivity();
        metricsService.simulateRegionalActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", generateFakeUsers());
        response.put("total", ThreadLocalRandom.current().nextInt(100, 1000));
        response.put("timestamp", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        metricsService.recordApiRequest("/api/users", duration);
        
        logger.info("GET /api/users processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users")
    @Timed(value = "api.users.post", description = "Create user endpoint timing")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody(required = false) Map<String, Object> user) {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(100, 500);
        
        metricsService.recordUserRegistration();
        metricsService.simulateUserActivity();
        metricsService.simulateDatabaseActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", ThreadLocalRandom.current().nextLong(1000, 99999));
        response.put("name", faker.name().fullName());
        response.put("email", faker.internet().emailAddress());
        response.put("created", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        metricsService.recordApiRequest("/api/users", duration);
        
        logger.info("POST /api/users processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders")
    @Timed(value = "api.orders.get", description = "Get orders endpoint timing")
    public ResponseEntity<Map<String, Object>> getOrders() {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(75, 400);
        
        metricsService.simulateOrderProcessing();
        metricsService.simulateDatabaseActivity();
        metricsService.simulateRegionalActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("orders", generateFakeOrders());
        response.put("total", ThreadLocalRandom.current().nextInt(50, 500));
        response.put("timestamp", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        metricsService.recordApiRequest("/api/orders", duration);
        
        logger.info("GET /api/orders processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orders")
    @Timed(value = "api.orders.post", description = "Create order endpoint timing")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody(required = false) Map<String, Object> order) {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(200, 800);
        
        metricsService.simulateOrderProcessing();
        metricsService.simulateDatabaseActivity();
        metricsService.simulateUserActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", ThreadLocalRandom.current().nextLong(10000, 999999));
        response.put("total", ThreadLocalRandom.current().nextDouble(10.0, 500.0));
        response.put("status", "confirmed");
        response.put("created", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        metricsService.recordApiRequest("/api/orders", duration);
        
        logger.info("POST /api/orders processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products")
    @Timed(value = "api.products.get", description = "Get products endpoint timing")
    public ResponseEntity<Map<String, Object>> getProducts() {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(30, 200);
        
        metricsService.simulateUserActivity();
        metricsService.simulateDatabaseActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", generateFakeProducts());
        response.put("total", ThreadLocalRandom.current().nextInt(200, 2000));
        response.put("timestamp", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        metricsService.recordApiRequest("/api/products", duration);
        
        logger.info("GET /api/products processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments")
    @Timed(value = "api.payments.post", description = "Process payment endpoint timing")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody(required = false) Map<String, Object> payment) {
        Instant start = Instant.now();
        
        // Simulate longer processing for payments
        simulateDelay(500, 2000);
        
        metricsService.simulateOrderProcessing();
        metricsService.simulateDatabaseActivity();
        metricsService.simulateUserActivity();
        
        // Simulate payment failure occasionally (10% chance)
        boolean paymentSuccess = ThreadLocalRandom.current().nextDouble() > 0.1;
        
        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", ThreadLocalRandom.current().nextLong(100000, 9999999));
        response.put("status", paymentSuccess ? "success" : "failed");
        response.put("amount", ThreadLocalRandom.current().nextDouble(10.0, 1000.0));
        response.put("processed", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        metricsService.recordApiRequest("/api/payments", duration);
        
        logger.info("POST /api/payments processed in {}ms with status: {}", 
                   duration.toMillis(), response.get("status"));
        
        if (paymentSuccess) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(402).body(response);
        }
    }

    @GetMapping("/health-check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        metricsService.simulateUserActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("timestamp", Instant.now());
        response.put("uptime", "running");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/simulate/{type}")
    public ResponseEntity<Map<String, Object>> simulateActivity(@PathVariable String type) {
        Map<String, Object> response = new HashMap<>();
        
        switch (type.toLowerCase()) {
            case "users":
                metricsService.simulateUserActivity();
                response.put("message", "User activity simulated");
                break;
            case "orders":
                metricsService.simulateOrderProcessing();
                response.put("message", "Order processing simulated");
                break;
            case "database":
                metricsService.simulateDatabaseActivity();
                response.put("message", "Database activity simulated");
                break;
            case "regions":
                metricsService.simulateRegionalActivity();
                response.put("message", "Regional activity simulated");
                break;
            case "endpoints":
                metricsService.simulateEndpointActivity();
                response.put("message", "Endpoint activity simulated");
                break;
            case "all":
                metricsService.simulateUserActivity();
                metricsService.simulateOrderProcessing();
                metricsService.simulateDatabaseActivity();
                metricsService.simulateRegionalActivity();
                metricsService.simulateEndpointActivity();
                response.put("message", "All activities simulated");
                break;
            default:
                response.put("error", "Unknown simulation type");
                return ResponseEntity.badRequest().body(response);
        }
        
        response.put("type", type);
        response.put("timestamp", Instant.now());
        
        logger.info("Simulated activity: {}", type);
        return ResponseEntity.ok(response);
    }

    private void simulateDelay(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Object generateFakeUsers() {
        int count = ThreadLocalRandom.current().nextInt(5, 15);
        Map<String, Object>[] users = new Map[count];
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", ThreadLocalRandom.current().nextLong(1, 10000));
            user.put("name", faker.name().fullName());
            user.put("email", faker.internet().emailAddress());
            user.put("city", faker.address().city());
            users[i] = user;
        }
        
        return users;
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
            products[i] = product;
        }
        
        return products;
    }

    private String getRandomOrderStatus() {
        String[] statuses = {"pending", "processing", "shipped", "delivered", "cancelled"};
        return statuses[ThreadLocalRandom.current().nextInt(statuses.length)];
    }
}
