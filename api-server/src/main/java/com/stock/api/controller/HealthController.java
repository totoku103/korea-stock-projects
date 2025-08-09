package com.stock.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 서버 상태 확인용 헬스 체크 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    
    @GetMapping
    public Mono<Map<String, Object>> health() {
        return Mono.just(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "service", "stock-api-server"
        ));
    }
}