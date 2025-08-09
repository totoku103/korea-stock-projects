package com.stock.batch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@Configuration
public class KisApiLoggingConfig {
    
    private static final Logger apiLogger = LoggerFactory.getLogger("KIS_API_CALL");
    private static final Logger performanceLogger = LoggerFactory.getLogger("KIS_API_PERFORMANCE");
    
    @Bean
    public ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            LocalDateTime startTime = LocalDateTime.now();
            
            // 요청 로깅
            apiLogger.info("API Request: {} {}", 
                clientRequest.method(), 
                clientRequest.url());
            
            apiLogger.debug("Request Headers: {}", 
                clientRequest.headers());
            
            // 시작 시간을 로깅 (속성 저장 대신)
            performanceLogger.debug("Request started at: {} for {}", 
                startTime, clientRequest.url());
            
            return Mono.just(clientRequest);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            
            // 응답 로깅
            LocalDateTime endTime = LocalDateTime.now();
            
            apiLogger.info("API Response: {} - Status: {}", 
                clientResponse.request().getURI(), 
                clientResponse.statusCode());
            
            performanceLogger.debug("Response received at: {} for {}", 
                endTime, clientResponse.request().getURI());
            
            return Mono.just(clientResponse);
        }));
    }
    
    @Bean
    public ExchangeFilterFunction errorHandlingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                return clientResponse.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> {
                        apiLogger.error("API Error Response: {} - Status: {} - Body: {}", 
                            clientResponse.request().getURI(), 
                            clientResponse.statusCode(), 
                            body);
                        return Mono.just(clientResponse);
                    });
            }
            return Mono.just(clientResponse);
        });
    }
    
    @Bean
    public ExchangeFilterFunction rateLimitFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().value() == 429) {
                String retryAfter = clientResponse.headers().header("Retry-After")
                    .stream()
                    .findFirst()
                    .orElse("60");
                
                apiLogger.warn("Rate limit exceeded for: {} - Retry after: {} seconds", 
                    clientResponse.request().getURI(), 
                    retryAfter);
            }
            return Mono.just(clientResponse);
        });
    }
}