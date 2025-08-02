package com.stock.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "kis.api")
public record KisApiProperties(
    String appKey,
    String appSecret,
    String accountNumber,
    String environment, // "real" or "mock"
    String baseUrl,
    String wsUrl,
    RateLimit rateLimit,
    Timeout timeout
) {
    
    @ConstructorBinding
    public KisApiProperties {
        if (appKey == null || appKey.isBlank()) {
            throw new IllegalArgumentException("KIS API App Key is required");
        }
        if (appSecret == null || appSecret.isBlank()) {
            throw new IllegalArgumentException("KIS API App Secret is required");
        }
        if (environment == null || environment.isBlank()) {
            environment = "mock";
        }
    }
    
    public record RateLimit(
        int requestsPerMinute,
        int requestsPerDay,
        int maxConcurrentConnections
    ) {
        public RateLimit {
            if (requestsPerMinute <= 0) {
                requestsPerMinute = 20;
            }
            if (requestsPerDay <= 0) {
                requestsPerDay = 10000;
            }
            if (maxConcurrentConnections <= 0) {
                maxConcurrentConnections = 5;
            }
        }
    }
    
    public record Timeout(
        int connectionTimeoutMs,
        int readTimeoutMs,
        int writeTimeoutMs
    ) {
        public Timeout {
            if (connectionTimeoutMs <= 0) {
                connectionTimeoutMs = 5000;
            }
            if (readTimeoutMs <= 0) {
                readTimeoutMs = 30000;
            }
            if (writeTimeoutMs <= 0) {
                writeTimeoutMs = 30000;
            }
        }
    }
    
    public boolean isRealEnvironment() {
        return "real".equalsIgnoreCase(environment);
    }
    
    public boolean isMockEnvironment() {
        return "mock".equalsIgnoreCase(environment);
    }
}