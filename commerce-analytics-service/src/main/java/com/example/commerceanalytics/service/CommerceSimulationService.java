package com.example.commerceanalytics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@ConditionalOnProperty(name = "app.metrics.simulation.enabled", havingValue = "true", matchIfMissing = true)
public class CommerceSimulationService {

    private static final Logger logger = LoggerFactory.getLogger(CommerceSimulationService.class);
    private final CommerceMetricsService commerceMetricsService;

    public CommerceSimulationService(CommerceMetricsService commerceMetricsService) {
        this.commerceMetricsService = commerceMetricsService;
    }

    /**
     * Simulates order processing every 12 seconds
     */
    @Scheduled(fixedRate = 12000)
    public void simulateBackgroundOrderProcessing() {
        try {
            // Simulate 1-4 orders per cycle
            int orders = ThreadLocalRandom.current().nextInt(1, 5);
            for (int i = 0; i < orders; i++) {
                commerceMetricsService.simulateOrderProcessing();
            }
            
            logger.debug("Simulated {} order processes", orders);
        } catch (Exception e) {
            logger.error("Error during order processing simulation", e);
        }
    }

    /**
     * Simulates payment processing every 8 seconds
     */
    @Scheduled(fixedRate = 8000)
    public void simulateBackgroundPaymentProcessing() {
        try {
            // Simulate 1-3 payments per cycle
            int payments = ThreadLocalRandom.current().nextInt(1, 4);
            for (int i = 0; i < payments; i++) {
                commerceMetricsService.simulatePaymentProcessing();
            }
            
            logger.debug("Simulated {} payment processes", payments);
        } catch (Exception e) {
            logger.error("Error during payment processing simulation", e);
        }
    }

    /**
     * Simulates product activity every 5 seconds
     */
    @Scheduled(fixedRate = 5000)
    public void simulateBackgroundProductActivity() {
        try {
            // Simulate 2-8 product activities per cycle
            int activities = ThreadLocalRandom.current().nextInt(2, 9);
            for (int i = 0; i < activities; i++) {
                commerceMetricsService.simulateProductActivity();
            }
            
            logger.debug("Simulated {} product activities", activities);
        } catch (Exception e) {
            logger.error("Error during product activity simulation", e);
        }
    }

    /**
     * Simulates cart activities every 7 seconds
     */
    @Scheduled(fixedRate = 7000)
    public void simulateBackgroundCartActivity() {
        try {
            // Simulate 1-5 cart activities per cycle
            int cartActions = ThreadLocalRandom.current().nextInt(1, 6);
            for (int i = 0; i < cartActions; i++) {
                commerceMetricsService.simulateCartActivity();
            }
            
            logger.debug("Simulated {} cart activities", cartActions);
        } catch (Exception e) {
            logger.error("Error during cart activity simulation", e);
        }
    }

    /**
     * Simulates database activity every 4 seconds
     */
    @Scheduled(fixedRate = 4000)
    public void simulateBackgroundDatabaseActivity() {
        try {
            // Simulate 2-6 database operations per cycle
            int dbOps = ThreadLocalRandom.current().nextInt(2, 7);
            for (int i = 0; i < dbOps; i++) {
                commerceMetricsService.simulateDatabaseActivity();
            }
            
            logger.debug("Simulated {} database operations", dbOps);
        } catch (Exception e) {
            logger.error("Error during database activity simulation", e);
        }
    }

    /**
     * Simulates regional commerce traffic every 9 seconds
     */
    @Scheduled(fixedRate = 9000)
    public void simulateBackgroundRegionalCommerceActivity() {
        try {
            // Simulate regional commerce traffic distribution
            int regionalRequests = ThreadLocalRandom.current().nextInt(3, 10);
            for (int i = 0; i < regionalRequests; i++) {
                commerceMetricsService.simulateCommerceRegionalActivity();
            }
            
            logger.debug("Simulated {} regional commerce requests", regionalRequests);
        } catch (Exception e) {
            logger.error("Error during regional commerce activity simulation", e);
        }
    }

    /**
     * Simulates commerce endpoint activity every 6 seconds
     */
    @Scheduled(fixedRate = 6000)
    public void simulateBackgroundCommerceEndpointActivity() {
        try {
            // Simulate various commerce endpoint calls
            int endpointCalls = ThreadLocalRandom.current().nextInt(2, 7);
            for (int i = 0; i < endpointCalls; i++) {
                commerceMetricsService.simulateCommerceEndpointActivity();
            }
            
            logger.debug("Simulated {} commerce endpoint calls", endpointCalls);
        } catch (Exception e) {
            logger.error("Error during commerce endpoint activity simulation", e);
        }
    }

    /**
     * Comprehensive commerce metrics simulation that runs every minute
     */
    @Scheduled(fixedRate = 60000)
    public void comprehensiveCommerceMetricsSimulation() {
        try {
            logger.info("Running comprehensive commerce metrics simulation cycle");
            
            // Ensure all commerce metric types are being generated
            commerceMetricsService.simulateOrderProcessing();
            commerceMetricsService.simulatePaymentProcessing();
            commerceMetricsService.simulateProductActivity();
            commerceMetricsService.simulateCartActivity();
            commerceMetricsService.simulateDatabaseActivity();
            commerceMetricsService.simulateCommerceRegionalActivity();
            commerceMetricsService.simulateCommerceEndpointActivity();
            
            logger.info("Comprehensive commerce metrics simulation cycle completed");
        } catch (Exception e) {
            logger.error("Error during comprehensive commerce metrics simulation", e);
        }
    }

    /**
     * Commerce activity burst simulation every 3 minutes (for sales events)
     */
    @Scheduled(fixedRate = 180000)
    public void simulateCommerceBurstActivity() {
        try {
            logger.info("Simulating commerce burst activity (sales event)");
            
            // Create a burst of commerce activity
            int burstSize = ThreadLocalRandom.current().nextInt(20, 50);
            for (int i = 0; i < burstSize; i++) {
                // Mix different types of commerce activities
                double activityType = ThreadLocalRandom.current().nextDouble();
                
                if (activityType < 0.25) {
                    commerceMetricsService.simulateOrderProcessing();
                } else if (activityType < 0.45) {
                    commerceMetricsService.simulatePaymentProcessing();
                } else if (activityType < 0.7) {
                    commerceMetricsService.simulateProductActivity();
                } else if (activityType < 0.85) {
                    commerceMetricsService.simulateCartActivity();
                } else {
                    commerceMetricsService.simulateCommerceEndpointActivity();
                }
                
                // Small delay between burst activities
                Thread.sleep(ThreadLocalRandom.current().nextInt(5, 30));
            }
            
            logger.info("Commerce burst activity simulation completed with {} activities", burstSize);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Commerce burst activity simulation interrupted");
        } catch (Exception e) {
            logger.error("Error during commerce burst activity simulation", e);
        }
    }
}
