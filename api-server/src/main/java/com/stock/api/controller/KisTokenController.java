package com.stock.api.controller;

import com.stock.common.dto.ApiResponse;
import com.stock.common.dto.KisTokenResponse;
import com.stock.common.dto.TokenStatusResponse;
import com.stock.common.service.KisTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/kis/token")
@Tag(name = "KIS Token Management", description = "한국투자증권 토큰 관리 API")
public class KisTokenController {
    
    private static final Logger log = LoggerFactory.getLogger(KisTokenController.class);
    
    private final KisTokenService kisTokenService;
    
    public KisTokenController(KisTokenService kisTokenService) {
        this.kisTokenService = kisTokenService;
    }
    
    @PostMapping("/issue")
    @Operation(summary = "접근 토큰 발급", description = "새로운 OAuth2 접근 토큰을 발급받습니다.")
    public Mono<ResponseEntity<ApiResponse<KisTokenResponse>>> issueToken() {
        log.info("토큰 발급 API 호출");
        
        return kisTokenService.issueNewAccessToken()
            .map(token -> {
                log.info("토큰 발급 성공: expiresAt={}", token.getExpiresAt());
                return ResponseEntity.ok(ApiResponse.success("토큰 발급 성공", token));
            })
            .onErrorResume(error -> {
                log.error("토큰 발급 실패", error);
                ApiResponse<KisTokenResponse> errorResponse = ApiResponse.error("토큰 발급 실패: " + error.getMessage());
                return Mono.just(ResponseEntity.badRequest().body(errorResponse));
            });
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "캐시된 토큰을 확인하고 필요시 새로 발급받습니다.")
    public Mono<ResponseEntity<ApiResponse<KisTokenResponse>>> refreshToken() {
        log.info("토큰 갱신 API 호출");
        
        return kisTokenService.getAccessToken()
            .map(token -> {
                log.info("토큰 갱신 성공: expiresAt={}", token.getExpiresAt());
                return ResponseEntity.ok(ApiResponse.success("토큰 갱신 성공", token));
            })
            .onErrorResume(error -> {
                log.error("토큰 갱신 실패", error);
                ApiResponse<KisTokenResponse> errorResponse = ApiResponse.error("토큰 갱신 실패: " + error.getMessage());
                return Mono.just(ResponseEntity.badRequest().body(errorResponse));
            });
    }
    
    @DeleteMapping("/revoke")
    @Operation(summary = "토큰 무효화", description = "현재 토큰을 무효화하고 캐시에서 제거합니다.")
    public Mono<ResponseEntity<ApiResponse<Void>>> revokeToken(@RequestParam String token) {
        log.info("토큰 무효화 API 호출");
        
        return kisTokenService.revokeToken(token)
            .then(Mono.fromRunnable(() -> log.info("토큰 무효화 성공")))
            .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success("토큰 무효화 성공", null))))
            .onErrorResume(error -> {
                log.error("토큰 무효화 실패", error);
                ApiResponse<Void> errorResponse = ApiResponse.error("토큰 무효화 실패: " + error.getMessage());
                return Mono.just(ResponseEntity.badRequest().body(errorResponse));
            });
    }
    
    @DeleteMapping("/invalidate-cache")
    @Operation(summary = "토큰 캐시 무효화", description = "캐시된 토큰을 강제로 무효화합니다.")
    public Mono<ResponseEntity<ApiResponse<Void>>> invalidateTokenCache() {
        log.info("토큰 캐시 무효화 API 호출");
        
        return Mono.fromRunnable(() -> {
            kisTokenService.invalidateCache();
            log.info("토큰 캐시 무효화 성공");
        })
        .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success("토큰 캐시 무효화 성공", null))))
        .onErrorResume(error -> {
            log.error("토큰 캐시 무효화 실패", error);
            ApiResponse<Void> errorResponse = ApiResponse.error("토큰 캐시 무효화 실패: " + error.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        });
    }
    
    @GetMapping("/status")
    @Operation(summary = "토큰 상태 조회", description = "현재 토큰의 상태를 조회합니다.")
    public Mono<ResponseEntity<ApiResponse<TokenStatusResponse>>> getTokenStatus() {
        log.info("토큰 상태 조회 API 호출");
        
        return Mono.fromCallable(() -> {
            KisTokenResponse cachedToken = kisTokenService.getCachedToken();
            boolean hasValidToken = kisTokenService.hasValidToken();
            log.info("토큰 상태 조회 완료: hasValidToken={}", hasValidToken);
            
            if (hasValidToken && cachedToken != null) {
                return TokenStatusResponse.fromTokenResponse(cachedToken);
            } else {
                return TokenStatusResponse.noToken();
            }
        })
        .map(status -> ResponseEntity.ok(ApiResponse.success("토큰 상태 조회 성공", status)))
        .onErrorResume(error -> {
            log.error("토큰 상태 조회 실패", error);
            ApiResponse<TokenStatusResponse> errorResponse = ApiResponse.error("토큰 상태 조회 실패: " + error.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        });
    }
}