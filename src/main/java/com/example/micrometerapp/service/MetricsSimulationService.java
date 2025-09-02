package com.example.micrometerapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@ConditionalOnProperty(name = "app.metrics.simulation.enabled", havingValue = "true", matchIfMissing = true)
public class MetricsSimulationService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsSimulationService.class);
    private final MetricsService metricsService;

    public MetricsSimulationService(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    /**
     * Simulates continuous user activity every 5 seconds
     */
    @Scheduled(fixedRateString = "${app.metrics.simulation.interval-seconds:5}000")
    public void simulateBackgroundUserActivity() {
        try {
            // Simulate multiple user activities in each cycle
            int activities = ThreadLocalRandom.current().nextInt(1, 5);
            for (int i = 0; i < activities; i++) {
                metricsService.simulateUserActivity();
            }
            
            logger.debug("Simulated {} user activities", activities);
        } catch (Exception e) {
            logger.error("Error during user activity simulation", e);
        }
    }

    /**
     * Simulates order processing every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    public void simulateBackgroundOrderProcessing() {
        try {
            // Simulate 1-3 orders per cycle
            int orders = ThreadLocalRandom.current().nextInt(1, 4);
            for (int i = 0; i < orders; i++) {
                metricsService.simulateOrderProcessing();
            }
            
            logger.debug("Simulated {} order processes", orders);
        } catch (Exception e) {
            logger.error("Error during order processing simulation", e);
        }
    }

    /**
     * Simulates database activity every 3 seconds
     */
    @Scheduled(fixedRate = 3000)
    public void simulateBackgroundDatabaseActivity() {
        try {
            // Simulate 2-6 database operations per cycle
            int dbOps = ThreadLocalRandom.current().nextInt(2, 7);
            for (int i = 0; i < dbOps; i++) {
                metricsService.simulateDatabaseActivity();
            }
            
            logger.debug("Simulated {} database operations", dbOps);
        } catch (Exception e) {
            logger.error("Error during database activity simulation", e);
        }
    }

    /**
     * Simulates regional traffic every 7 seconds
     */
    @Scheduled(fixedRate = 7000)
    public void simulateBackgroundRegionalActivity() {
        try {
            // Simulate regional traffic distribution
            int regionalRequests = ThreadLocalRandom.current().nextInt(3, 8);
            for (int i = 0; i < regionalRequests; i++) {
                metricsService.simulateRegionalActivity();
            }
            
            logger.debug("Simulated {} regional requests", regionalRequests);
        } catch (Exception e) {
            logger.error("Error during regional activity simulation", e);
        }
    }

    /**
     * Simulates endpoint activity every 8 seconds
     */
    @Scheduled(fixedRate = 8000)
    public void simulateBackgroundEndpointActivity() {
        try {
            // Simulate various endpoint calls
            int endpointCalls = ThreadLocalRandom.current().nextInt(2, 6);
            for (int i = 0; i < endpointCalls; i++) {
                metricsService.simulateEndpointActivity();
            }
            
            logger.debug("Simulated {} endpoint calls", endpointCalls);
        } catch (Exception e) {
            logger.error("Error during endpoint activity simulation", e);
        }
    }

    /**
     * Simulates periodic user registrations every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void simulateBackgroundUserRegistrations() {
        try {
            // Simulate 0-3 user registrations per cycle (sometimes none)
            int registrations = ThreadLocalRandom.current().nextInt(0, 4);
            for (int i = 0; i < registrations; i++) {
                metricsService.recordUserRegistration();
            }
            
            if (registrations > 0) {
                logger.debug("Simulated {} user registrations", registrations);
            }
        } catch (Exception e) {
            logger.error("Error during user registration simulation", e);
        }
    }

    /**
     * Comprehensive simulation that runs every minute to ensure all metrics are being generated
     */
    @Scheduled(fixedRate = 60000)
    public void comprehensiveMetricsSimulation() {
        try {
            logger.info("Running comprehensive metrics simulation cycle");
            
            // Ensure all metric types are being generated
            metricsService.simulateUserActivity();
            metricsService.simulateOrderProcessing();
            metricsService.simulateDatabaseActivity();
            metricsService.simulateRegionalActivity();
            metricsService.simulateEndpointActivity();
            
            logger.info("Comprehensive metrics simulation cycle completed");
        } catch (Exception e) {
            logger.error("Error during comprehensive metrics simulation", e);
        }
    }

    /**
     * Burst activity simulation every 2 minutes to create interesting patterns
     */
    @Scheduled(fixedRate = 120000)
    public void simulateBurstActivity() {
        try {
            logger.info("Simulating burst activity");
            
            // Create a burst of activity
            int burstSize = ThreadLocalRandom.current().nextInt(10, 25);
            for (int i = 0; i < burstSize; i++) {
                // Mix different types of activities
                double activityType = ThreadLocalRandom.current().nextDouble();
                
                if (activityType < 0.3) {
                    metricsService.simulateUserActivity();
                } else if (activityType < 0.5) {
                    metricsService.simulateOrderProcessing();
                } else if (activityType < 0.7) {
                    metricsService.simulateDatabaseActivity();
                } else if (activityType < 0.85) {
                    metricsService.simulateRegionalActivity();
                } else {
                    metricsService.simulateEndpointActivity();
                }
                
                // Small delay between burst activities
                Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
            }
            
            logger.info("Burst activity simulation completed with {} activities", burstSize);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Burst activity simulation interrupted");
        } catch (Exception e) {
            logger.error("Error during burst activity simulation", e);
        }
    }
}
