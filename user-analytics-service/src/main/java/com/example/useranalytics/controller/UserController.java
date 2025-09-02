package com.example.useranalytics.controller;

import com.example.useranalytics.service.UserMetricsService;
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
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserMetricsService userMetricsService;
    private final Faker faker = new Faker();

    public UserController(UserMetricsService userMetricsService) {
        this.userMetricsService = userMetricsService;
    }

    @GetMapping("/users")
    @Timed(value = "api.users.get", description = "Get users endpoint timing")
    public ResponseEntity<Map<String, Object>> getUsers() {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(50, 300);
        
        userMetricsService.simulateUserActivity();
        userMetricsService.simulateUserRegionalActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", generateFakeUsers());
        response.put("total", ThreadLocalRandom.current().nextInt(100, 1000));
        response.put("timestamp", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        userMetricsService.recordUserApiRequest("/api/users", duration);
        
        logger.info("GET /api/users processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users")
    @Timed(value = "api.users.post", description = "Create user endpoint timing")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody(required = false) Map<String, Object> user) {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(100, 500);
        
        userMetricsService.recordUserRegistration();
        userMetricsService.simulateUserActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", ThreadLocalRandom.current().nextLong(1000, 99999));
        response.put("name", faker.name().fullName());
        response.put("email", faker.internet().emailAddress());
        response.put("created", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        userMetricsService.recordUserApiRequest("/api/users", duration);
        
        logger.info("POST /api/users processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/profile")
    @Timed(value = "api.users.profile.get", description = "Get user profile endpoint timing")
    public ResponseEntity<Map<String, Object>> getUserProfile(@RequestParam(defaultValue = "1") Long userId) {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(75, 400);
        
        userMetricsService.simulateUserActivity();
        userMetricsService.simulateUserEndpointActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("name", faker.name().fullName());
        response.put("email", faker.internet().emailAddress());
        response.put("profileViews", ThreadLocalRandom.current().nextInt(1, 100));
        response.put("lastLogin", Instant.now().minusSeconds(ThreadLocalRandom.current().nextLong(3600)));
        response.put("timestamp", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        userMetricsService.recordUserApiRequest("/api/users/profile", duration);
        
        logger.info("GET /api/users/profile processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/auth")
    @Timed(value = "api.users.auth.post", description = "User authentication endpoint timing")
    public ResponseEntity<Map<String, Object>> authenticateUser(@RequestBody(required = false) Map<String, Object> credentials) {
        Instant start = Instant.now();
        
        // Simulate authentication delay
        simulateDelay(200, 800);
        
        userMetricsService.recordUserLogin();
        userMetricsService.simulateUserActivity();
        
        // Simulate auth success/failure
        boolean authSuccess = ThreadLocalRandom.current().nextDouble() > 0.15; // 15% failure rate
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", authSuccess);
        response.put("userId", authSuccess ? ThreadLocalRandom.current().nextLong(1000, 99999) : null);
        response.put("token", authSuccess ? faker.internet().uuid() : null);
        response.put("timestamp", Instant.now());
        
        Duration duration = Duration.between(start, Instant.now());
        userMetricsService.recordUserApiRequest("/api/users/auth", duration);
        
        logger.info("POST /api/users/auth processed in {}ms with success: {}", duration.toMillis(), authSuccess);
        
        if (authSuccess) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/users/sessions")
    @Timed(value = "api.users.sessions.post", description = "Create user session endpoint timing")
    public ResponseEntity<Map<String, Object>> createUserSession(@RequestBody(required = false) Map<String, Object> sessionData) {
        Instant start = Instant.now();
        
        // Simulate processing delay
        simulateDelay(50, 200);
        
        userMetricsService.recordUserSession();
        userMetricsService.simulateUserActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", faker.internet().uuid());
        response.put("userId", ThreadLocalRandom.current().nextLong(1000, 99999));
        response.put("started", Instant.now());
        response.put("expiresAt", Instant.now().plusSeconds(3600)); // 1 hour
        
        Duration duration = Duration.between(start, Instant.now());
        userMetricsService.recordUserApiRequest("/api/users/sessions", duration);
        
        logger.info("POST /api/users/sessions processed in {}ms", duration.toMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health-check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        userMetricsService.simulateUserActivity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "user-analytics");
        response.put("timestamp", Instant.now());
        response.put("uptime", "running");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/simulate/{type}")
    public ResponseEntity<Map<String, Object>> simulateActivity(@PathVariable String type) {
        Map<String, Object> response = new HashMap<>();
        
        switch (type.toLowerCase()) {
            case "users":
                userMetricsService.simulateUserActivity();
                response.put("message", "User activity simulated");
                break;
            case "registrations":
                userMetricsService.recordUserRegistration();
                response.put("message", "User registration simulated");
                break;
            case "logins":
                userMetricsService.recordUserLogin();
                response.put("message", "User login simulated");
                break;
            case "sessions":
                userMetricsService.recordUserSession();
                response.put("message", "User session simulated");
                break;
            case "regions":
                userMetricsService.simulateUserRegionalActivity();
                response.put("message", "Regional user activity simulated");
                break;
            case "endpoints":
                userMetricsService.simulateUserEndpointActivity();
                response.put("message", "User endpoint activity simulated");
                break;
            case "all":
                userMetricsService.simulateUserActivity();
                userMetricsService.recordUserRegistration();
                userMetricsService.recordUserLogin();
                userMetricsService.recordUserSession();
                userMetricsService.simulateUserRegionalActivity();
                userMetricsService.simulateUserEndpointActivity();
                response.put("message", "All user activities simulated");
                break;
            default:
                response.put("error", "Unknown simulation type");
                return ResponseEntity.badRequest().body(response);
        }
        
        response.put("type", type);
        response.put("service", "user-analytics");
        response.put("timestamp", Instant.now());
        
        logger.info("Simulated user activity: {}", type);
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
            user.put("joinDate", faker.date().birthday());
            user.put("userType", getRandomUserType());
            users[i] = user;
        }
        
        return users;
    }
    
    private String getRandomUserType() {
        String[] types = {"free", "premium", "enterprise", "trial"};
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }
}
