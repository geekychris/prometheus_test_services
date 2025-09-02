package com.example.useranalytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@ConditionalOnProperty(name = "app.metrics.simulation.enabled", havingValue = "true", matchIfMissing = true)
public class UserSimulationService {

    private static final Logger logger = LoggerFactory.getLogger(UserSimulationService.class);
    private final UserMetricsService userMetricsService;

    public UserSimulationService(UserMetricsService userMetricsService) {
        this.userMetricsService = userMetricsService;
    }

    /**
     * Simulates continuous user activity every 4 seconds
     */
    @Scheduled(fixedRate = 4000)
    public void simulateBackgroundUserActivity() {
        try {
            // Simulate multiple user activities in each cycle
            int activities = ThreadLocalRandom.current().nextInt(2, 6);
            for (int i = 0; i < activities; i++) {
                userMetricsService.simulateUserActivity();
            }
            
            logger.debug("Simulated {} user activities", activities);
        } catch (Exception e) {
            logger.error("Error during user activity simulation", e);
        }
    }

    /**
     * Simulates user registrations every 25 seconds
     */
    @Scheduled(fixedRate = 25000)
    public void simulateBackgroundUserRegistrations() {
        try {
            // Simulate 0-4 user registrations per cycle
            int registrations = ThreadLocalRandom.current().nextInt(0, 5);
            for (int i = 0; i < registrations; i++) {
                userMetricsService.recordUserRegistration();
            }
            
            if (registrations > 0) {
                logger.debug("Simulated {} user registrations", registrations);
            }
        } catch (Exception e) {
            logger.error("Error during user registration simulation", e);
        }
    }

    /**
     * Simulates user logins every 15 seconds
     */
    @Scheduled(fixedRate = 15000)
    public void simulateBackgroundUserLogins() {
        try {
            // Simulate 1-5 user logins per cycle
            int logins = ThreadLocalRandom.current().nextInt(1, 6);
            for (int i = 0; i < logins; i++) {
                userMetricsService.recordUserLogin();
            }
            
            logger.debug("Simulated {} user logins", logins);
        } catch (Exception e) {
            logger.error("Error during user login simulation", e);
        }
    }

    /**
     * Simulates user sessions every 20 seconds
     */
    @Scheduled(fixedRate = 20000)
    public void simulateBackgroundUserSessions() {
        try {
            // Simulate 1-3 user sessions per cycle
            int sessions = ThreadLocalRandom.current().nextInt(1, 4);
            for (int i = 0; i < sessions; i++) {
                userMetricsService.recordUserSession();
            }
            
            logger.debug("Simulated {} user sessions", sessions);
        } catch (Exception e) {
            logger.error("Error during user session simulation", e);
        }
    }

    /**
     * Simulates regional user traffic every 8 seconds
     */
    @Scheduled(fixedRate = 8000)
    public void simulateBackgroundRegionalUserActivity() {
        try {
            // Simulate regional user traffic distribution
            int regionalRequests = ThreadLocalRandom.current().nextInt(2, 8);
            for (int i = 0; i < regionalRequests; i++) {
                userMetricsService.simulateUserRegionalActivity();
            }
            
            logger.debug("Simulated {} regional user requests", regionalRequests);
        } catch (Exception e) {
            logger.error("Error during regional user activity simulation", e);
        }
    }

    /**
     * Simulates user endpoint activity every 6 seconds
     */
    @Scheduled(fixedRate = 6000)
    public void simulateBackgroundUserEndpointActivity() {
        try {
            // Simulate various user endpoint calls
            int endpointCalls = ThreadLocalRandom.current().nextInt(1, 5);
            for (int i = 0; i < endpointCalls; i++) {
                userMetricsService.simulateUserEndpointActivity();
            }
            
            logger.debug("Simulated {} user endpoint calls", endpointCalls);
        } catch (Exception e) {
            logger.error("Error during user endpoint activity simulation", e);
        }
    }

    /**
     * Comprehensive user metrics simulation that runs every minute
     */
    @Scheduled(fixedRate = 60000)
    public void comprehensiveUserMetricsSimulation() {
        try {
            logger.info("Running comprehensive user metrics simulation cycle");
            
            // Ensure all user metric types are being generated
            userMetricsService.simulateUserActivity();
            userMetricsService.recordUserRegistration();
            userMetricsService.recordUserLogin();
            userMetricsService.recordUserSession();
            userMetricsService.simulateUserRegionalActivity();
            userMetricsService.simulateUserEndpointActivity();
            
            logger.info("Comprehensive user metrics simulation cycle completed");
        } catch (Exception e) {
            logger.error("Error during comprehensive user metrics simulation", e);
        }
    }

    /**
     * User activity burst simulation every 2.5 minutes
     */
    @Scheduled(fixedRate = 150000)
    public void simulateUserBurstActivity() {
        try {
            logger.info("Simulating user burst activity");
            
            // Create a burst of user activity
            int burstSize = ThreadLocalRandom.current().nextInt(15, 35);
            for (int i = 0; i < burstSize; i++) {
                // Mix different types of user activities
                double activityType = ThreadLocalRandom.current().nextDouble();
                
                if (activityType < 0.3) {
                    userMetricsService.simulateUserActivity();
                } else if (activityType < 0.5) {
                    userMetricsService.recordUserLogin();
                } else if (activityType < 0.7) {
                    userMetricsService.recordUserSession();
                } else if (activityType < 0.85) {
                    userMetricsService.simulateUserRegionalActivity();
                } else {
                    userMetricsService.simulateUserEndpointActivity();
                }
                
                // Small delay between burst activities
                Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
            }
            
            logger.info("User burst activity simulation completed with {} activities", burstSize);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("User burst activity simulation interrupted");
        } catch (Exception e) {
            logger.error("Error during user burst activity simulation", e);
        }
    }
}
