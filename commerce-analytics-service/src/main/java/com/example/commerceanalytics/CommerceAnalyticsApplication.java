package com.example.commerceanalytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CommerceAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommerceAnalyticsApplication.class, args);
    }
}
