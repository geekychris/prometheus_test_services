package com.example.useranalytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UserAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserAnalyticsApplication.class, args);
    }
}
