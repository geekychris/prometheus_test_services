package com.example.micrometerapp.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class MetricsConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment environment) {
        return registry -> {
            // Add common tags that will be applied to all metrics
            registry.config()
                    .commonTags(
                        "application", environment.getProperty("spring.application.name", "micrometer-demo"),
                        "environment", environment.getProperty("app.environment", "development"),
                        "version", environment.getProperty("app.version", "1.0.0"),
                        "instance", environment.getProperty("app.instance", "local")
                    );
        };
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterFilters() {
        return registry -> {
            // Configure metric filters
            registry.config()
                    // Rename metrics if needed (example)
                    .meterFilter(MeterFilter.renameTag("http.server.requests", "uri", "endpoint"))
                    
                    // Deny certain metrics that might not be useful (example)
                    .meterFilter(MeterFilter.deny(id -> id.getName().startsWith("tomcat.threads")))
                    
                    // Accept only specific JVM metrics and our app metrics to reduce noise
                    .meterFilter(MeterFilter.accept(id -> 
                        id.getName().startsWith("jvm.memory") ||
                        id.getName().startsWith("jvm.gc") ||
                        id.getName().startsWith("system.cpu") ||
                        id.getName().startsWith("process.cpu") ||
                        id.getName().startsWith("http.server.requests") ||
                        id.getName().startsWith("app.")));
        };
    }
}
