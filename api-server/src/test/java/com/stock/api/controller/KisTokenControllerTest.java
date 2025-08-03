package com.stock.api.controller;

import com.stock.common.dto.KisTokenResponse;
import com.stock.common.dto.TokenStatusResponse;
import com.stock.common.exception.KisApiException;
import com.stock.common.service.KisTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = KisTokenController.class, 
             excludeAutoConfiguration = org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class)
class KisTokenControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private KisTokenService kisTokenService;
    
    private KisTokenResponse mockTokenResponse;
    
    @BeforeEach
    void setUp() {
        mockTokenResponse = new KisTokenResponse(
            "test-access-token",
            "Bearer",
            86400L,
            "2024-12-31 23:59:59"
        );
    }
    
    @Test
    void issueToken_성공() {
        // Given
        when(kisTokenService.issueNewAccessToken())
            .thenReturn(Mono.just(mockTokenResponse));
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/kis/token/issue")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("토큰 발급 성공")
            .jsonPath("$.data.access_token").isEqualTo("test-access-token")
            .jsonPath("$.data.token_type").isEqualTo("Bearer")
            .jsonPath("$.data.expires_in").isEqualTo(86400);
    }
    
    @Test
    void issueToken_실패() {
        // Given
        when(kisTokenService.issueNewAccessToken())
            .thenReturn(Mono.error(new KisApiException("API 호출 실패")));
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/kis/token/issue")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.message").value(message -> 
                ((String) message).contains("토큰 발급 실패"));
    }
    
    @Test
    void refreshToken_성공() {
        // Given
        when(kisTokenService.getAccessToken())
            .thenReturn(Mono.just(mockTokenResponse));
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/kis/token/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("토큰 갱신 성공")
            .jsonPath("$.data.access_token").isEqualTo("test-access-token");
    }
    
    @Test
    void refreshToken_실패() {
        // Given
        when(kisTokenService.getAccessToken())
            .thenReturn(Mono.error(new KisApiException("토큰 갱신 실패")));
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/kis/token/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.message").value(message -> 
                ((String) message).contains("토큰 갱신 실패"));
    }
    
    @Test
    void revokeToken_성공() {
        // Given
        when(kisTokenService.revokeToken(anyString()))
            .thenReturn(Mono.empty());
        
        // When & Then
        webTestClient.delete()
            .uri("/api/v1/kis/token/revoke?token=test-token")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("토큰 무효화 성공");
    }
    
    @Test
    void revokeToken_실패() {
        // Given
        when(kisTokenService.revokeToken(anyString()))
            .thenReturn(Mono.error(new KisApiException("토큰 무효화 실패")));
        
        // When & Then
        webTestClient.delete()
            .uri("/api/v1/kis/token/revoke?token=test-token")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.message").value(message -> 
                ((String) message).contains("토큰 무효화 실패"));
    }
    
    @Test
    void invalidateTokenCache_성공() {
        // When & Then
        webTestClient.delete()
            .uri("/api/v1/kis/token/invalidate-cache")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("토큰 캐시 무효화 성공");
    }
    
    @Test
    void getTokenStatus_유효한_토큰_있음() {
        // Given
        when(kisTokenService.hasValidToken()).thenReturn(true);
        when(kisTokenService.getCachedToken()).thenReturn(mockTokenResponse);
        
        // When & Then
        webTestClient.get()
            .uri("/api/v1/kis/token/status")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("토큰 상태 조회 성공")
            .jsonPath("$.data.has_valid_token").isEqualTo(true)
            .jsonPath("$.data.token_type").isEqualTo("Bearer")
            .jsonPath("$.data.expires_in_seconds").isEqualTo(86400);
    }
    
    @Test
    void getTokenStatus_유효한_토큰_없음() {
        // Given
        when(kisTokenService.hasValidToken()).thenReturn(false);
        when(kisTokenService.getCachedToken()).thenReturn(null);
        
        // When & Then
        webTestClient.get()
            .uri("/api/v1/kis/token/status")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("토큰 상태 조회 성공")
            .jsonPath("$.data.has_valid_token").isEqualTo(false)
            .jsonPath("$.data.token_type").isEmpty()
            .jsonPath("$.data.expires_in_seconds").isEqualTo(0);
    }
    
    @Test
    void getTokenStatus_실패() {
        // Given
        when(kisTokenService.hasValidToken()).thenThrow(new RuntimeException("상태 조회 실패"));
        
        // When & Then
        webTestClient.get()
            .uri("/api/v1/kis/token/status")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.message").value(message -> 
                ((String) message).contains("토큰 상태 조회 실패"));
    }
}