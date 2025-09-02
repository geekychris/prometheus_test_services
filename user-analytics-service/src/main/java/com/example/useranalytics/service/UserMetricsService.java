package com.example.useranalytics.service;

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
public class UserMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(UserMetricsService.class);
    private final Faker faker = new Faker();
    
    private final MeterRegistry meterRegistry;
    
    // User-specific Counters
    private final Counter userRegistrationCounter;
    private final Counter userLoginCounter;
    private final Counter userSessionCounter;
    private final Counter userEngagementCounter;
    
    // User-specific Gauges
    private final AtomicInteger activeUsers;
    private final AtomicInteger onlineUsers;
    private final AtomicLong totalSessionDuration;
    private final AtomicInteger userCacheSize;
    
    // User-specific Timers
    private final Timer userRequestTimer;
    private final Timer userAuthTimer;
    private final Timer userProfileLoadTimer;
    
    // Distribution Summary for user metrics
    private final DistributionSummary sessionDurationSummary;
    private final DistributionSummary userActivitySummary;
    
    // Regional user metrics
    private final Map<String, Counter> userRegionCounters = new HashMap<>();
    private final Map<String, Timer> userEndpointTimers = new HashMap<>();

    public UserMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize user-specific counters
        this.userRegistrationCounter = Counter.builder("user.registrations.total")
                .description("Total number of user registrations")
                .register(meterRegistry);
        
        this.userLoginCounter = Counter.builder("user.logins.total")
                .description("Total number of user logins")
                .register(meterRegistry);
        
        this.userSessionCounter = Counter.builder("user.sessions.total")
                .description("Total number of user sessions")
                .register(meterRegistry);
        
        this.userEngagementCounter = Counter.builder("user.engagement.total")
                .description("Total user engagement events")
                .register(meterRegistry);
        
        // Initialize atomic values for user gauges
        this.activeUsers = new AtomicInteger(0);
        this.onlineUsers = new AtomicInteger(0);
        this.totalSessionDuration = new AtomicLong(0);
        this.userCacheSize = new AtomicInteger(0);
        
        // Register user gauges
        Gauge.builder("user.active.count", activeUsers, AtomicInteger::doubleValue)
                .description("Number of currently active users")
                .register(meterRegistry);
        
        Gauge.builder("user.online.count", onlineUsers, AtomicInteger::doubleValue)
                .description("Number of currently online users")
                .register(meterRegistry);
        
        Gauge.builder("user.session.duration.total", totalSessionDuration, AtomicLong::doubleValue)
                .description("Total session duration in seconds")
                .register(meterRegistry);
        
        Gauge.builder("user.cache.size", userCacheSize, AtomicInteger::doubleValue)
                .description("Number of users in cache")
                .register(meterRegistry);
        
        // Initialize user timers
        this.userRequestTimer = Timer.builder("user.request.duration")
                .description("User request processing time")
                .register(meterRegistry);
        
        this.userAuthTimer = Timer.builder("user.auth.duration")
                .description("User authentication processing time")
                .register(meterRegistry);
        
        this.userProfileLoadTimer = Timer.builder("user.profile.load.duration")
                .description("User profile loading time")
                .register(meterRegistry);
        
        // Initialize user distribution summaries
        this.sessionDurationSummary = DistributionSummary.builder("user.session.duration")
                .description("Distribution of user session durations")
                .baseUnit("seconds")
                .register(meterRegistry);
        
        this.userActivitySummary = DistributionSummary.builder("user.activity.score")
                .description("Distribution of user activity scores")
                .register(meterRegistry);
        
        // Initialize regional user counters
        initializeUserRegionalCounters();
        initializeUserEndpointTimers();
    }
    
    private void initializeUserRegionalCounters() {
        String[] regions = {"us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1", "ap-northeast-1"};
        for (String region : regions) {
            userRegionCounters.put(region, Counter.builder("user.requests.by.region")
                    .tag("region", region)
                    .description("User requests by region")
                    .register(meterRegistry));
        }
    }
    
    private void initializeUserEndpointTimers() {
        String[] endpoints = {"/api/users", "/api/users/profile", "/api/users/auth", "/api/users/sessions"};
        for (String endpoint : endpoints) {
            userEndpointTimers.put(endpoint, Timer.builder("user.endpoint.duration")
                    .tag("endpoint", endpoint)
                    .description("User endpoint processing time")
                    .register(meterRegistry));
        }
    }
    
    public void simulateUserActivity() {
        // Update active users gauge
        int change = ThreadLocalRandom.current().nextInt(-3, 8);
        int newActiveValue = Math.max(0, Math.min(150, activeUsers.get() + change));
        activeUsers.set(newActiveValue);
        
        // Update online users (slightly higher than active)
        int onlineChange = ThreadLocalRandom.current().nextInt(-2, 10);
        int newOnlineValue = Math.max(newActiveValue, Math.min(200, onlineUsers.get() + onlineChange));
        onlineUsers.set(newOnlineValue);
        
        // Update user cache
        userCacheSize.set(ThreadLocalRandom.current().nextInt(50, 500));
        
        // Record user engagement
        userEngagementCounter.increment();
        
        // Record activity score
        double activityScore = ThreadLocalRandom.current().nextDouble(1.0, 10.0);
        userActivitySummary.record(activityScore);
        
        logger.debug("Updated user activity metrics - Active: {}, Online: {}", newActiveValue, newOnlineValue);
    }
    
    public void recordUserRegistration() {
        Counter.builder("user.registrations.total")
            .description("Total number of user registrations")
            .tag("source", getRandomRegistrationSource())
            .tag("user_type", getRandomUserType())
            .tag("device", getRandomDevice())
            .register(meterRegistry)
            .increment();
    }
    
    public void recordUserLogin() {
        Counter.builder("user.logins.total")
            .description("Total number of user logins")
            .tag("auth_method", getRandomAuthMethod())
            .tag("device", getRandomDevice())
            .tag("success", ThreadLocalRandom.current().nextBoolean() ? "true" : "false")
            .register(meterRegistry)
            .increment();
            
        // Record auth timing
        Duration authDuration = Duration.ofMillis(ThreadLocalRandom.current().nextLong(100, 1500));
        userAuthTimer.record(authDuration);
    }
    
    public void recordUserSession() {
        userSessionCounter.increment();
        
        // Record session duration
        long sessionDurationSeconds = ThreadLocalRandom.current().nextLong(60, 3600); // 1 min to 1 hour
        sessionDurationSummary.record(sessionDurationSeconds);
        totalSessionDuration.addAndGet(sessionDurationSeconds);
    }
    
    public void simulateUserRegionalActivity() {
        // Randomly select a region and increment its counter
        String[] regions = {"us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1", "ap-northeast-1"};
        String randomRegion = regions[ThreadLocalRandom.current().nextInt(regions.length)];
        userRegionCounters.get(randomRegion).increment();
    }
    
    public void simulateUserEndpointActivity() {
        // Randomly select a user endpoint and record its processing time
        String[] endpoints = {"/api/users", "/api/users/profile", "/api/users/auth", "/api/users/sessions"};
        String randomEndpoint = endpoints[ThreadLocalRandom.current().nextInt(endpoints.length)];
        Duration endpointDuration = Duration.ofMillis(ThreadLocalRandom.current().nextLong(25, 800));
        userEndpointTimers.get(randomEndpoint).record(endpointDuration);
    }
    
    public void recordUserApiRequest(String endpoint, Duration duration) {
        userRequestTimer.record(duration);
        
        Timer endpointTimer = userEndpointTimers.get(endpoint);
        if (endpointTimer != null) {
            endpointTimer.record(duration);
        }
    }
    
    private String getRandomRegistrationSource() {
        String[] sources = {"web", "mobile_app", "social_login", "referral", "api", "admin"};
        return sources[ThreadLocalRandom.current().nextInt(sources.length)];
    }
    
    private String getRandomUserType() {
        String[] types = {"free", "premium", "enterprise", "trial"};
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }
    
    private String getRandomAuthMethod() {
        String[] methods = {"password", "oauth", "sso", "2fa", "biometric"};
        return methods[ThreadLocalRandom.current().nextInt(methods.length)];
    }
    
    private String getRandomDevice() {
        String[] devices = {"desktop", "mobile", "tablet", "api"};
        return devices[ThreadLocalRandom.current().nextInt(devices.length)];
    }
}
