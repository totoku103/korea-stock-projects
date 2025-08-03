package com.stock.api.controller;

import com.stock.common.dto.ApiResponse;
import com.stock.common.dto.KisWebSocketKeyResponse;
import com.stock.common.service.KisWebSocketKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/kis/websocket")
@Tag(name = "KIS WebSocket Management", description = "한국투자증권 웹소켓 관리 API")
public class KisWebSocketController {
    
    private static final Logger log = LoggerFactory.getLogger(KisWebSocketController.class);
    
    private final KisWebSocketKeyService kisWebSocketKeyService;
    
    public KisWebSocketController(KisWebSocketKeyService kisWebSocketKeyService) {
        this.kisWebSocketKeyService = kisWebSocketKeyService;
    }
    
    @PostMapping("/approval-key")
    @Operation(summary = "웹소켓 접속키 발급", description = "실시간 데이터 수신을 위한 웹소켓 접속키를 발급받습니다.")
    public Mono<ResponseEntity<ApiResponse<KisWebSocketKeyResponse>>> getApprovalKey() {
        log.info("웹소켓 접속키 발급 API 호출");
        
        return kisWebSocketKeyService.getWebSocketApprovalKey()
            .map(response -> {
                if (response.isSuccessful()) {
                    log.info("웹소켓 접속키 발급 성공: messageCode={}", response.messageCode());
                    return ResponseEntity.ok(ApiResponse.success("웹소켓 접속키 발급 성공", response));
                } else {
                    log.warn("웹소켓 접속키 발급 실패: messageCode={}, message={}", 
                           response.messageCode(), response.message());
                    ApiResponse<KisWebSocketKeyResponse> errorResponse = 
                        ApiResponse.error("웹소켓 접속키 발급 실패: " + response.message(), 
                                        response.messageCode());
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            })
            .onErrorResume(error -> {
                log.error("웹소켓 접속키 발급 중 오류 발생", error);
                ApiResponse<KisWebSocketKeyResponse> errorResponse = 
                    ApiResponse.error("웹소켓 접속키 발급 실패: " + error.getMessage());
                return Mono.just(ResponseEntity.badRequest().body(errorResponse));
            });
    }
}