package com.stock.common.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.common.config.KisApiProperties;
import com.stock.common.dto.KisTokenResponse;
import com.stock.common.dto.KisWebSocketKeyResponse;
import com.stock.common.service.KisTokenService;
import com.stock.common.service.KisWebSocketKeyService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class KisApiIntegrationTest {
    
    private MockWebServer mockWebServer;
    private KisTokenService kisTokenService;
    private KisWebSocketKeyService kisWebSocketKeyService;
    private KisApiProperties kisApiProperties;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        String baseUrl = String.format("http://localhost:%d", mockWebServer.getPort());
        
        kisApiProperties = new KisApiProperties(
            "test-app-key",
            "test-app-secret",
            "01234567-89",
            "mock",
            baseUrl,
            "ws://localhost:31000",
            new KisApiProperties.RateLimit(20, 10000, 5),
            new KisApiProperties.Timeout(5000, 30000, 30000)
        );
        
        WebClient webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
            
        kisTokenService = new KisTokenService(webClient, kisApiProperties);
        kisWebSocketKeyService = new KisWebSocketKeyService(webClient, kisApiProperties);
        objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void 전체_워크플로우_토큰_발급_후_웹소켓_키_발급() throws Exception {
        // Given
        // 1. 토큰 발급 응답 설정
        KisTokenResponse tokenResponse = new KisTokenResponse(
            "access-token-12345",
            "Bearer",
            86400L,
            "2024-12-31 23:59:59"
        );
        
        MockResponse tokenMockResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(tokenResponse));
        
        // 2. 웹소켓 키 발급 응답 설정
        KisWebSocketKeyResponse wsKeyResponse = new KisWebSocketKeyResponse(
            "ws-approval-key-67890",
            "0",
            "정상처리 되었습니다."
        );
        
        MockResponse wsKeyMockResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(wsKeyResponse));
        
        mockWebServer.enqueue(tokenMockResponse);
        mockWebServer.enqueue(wsKeyMockResponse);
        
        // When & Then
        // 1. 토큰 발급
        Mono<KisTokenResponse> tokenResult = kisTokenService.getAccessToken();
        StepVerifier.create(tokenResult)
            .expectNextMatches(response -> {
                assertThat(response.accessToken()).isEqualTo("access-token-12345");
                assertThat(response.isExpired()).isFalse();
                return true;
            })
            .verifyComplete();
        
        // 2. 웹소켓 키 발급
        Mono<KisWebSocketKeyResponse> wsKeyResult = kisWebSocketKeyService.getWebSocketApprovalKey();
        StepVerifier.create(wsKeyResult)
            .expectNextMatches(response -> {
                assertThat(response.approvalKey()).isEqualTo("ws-approval-key-67890");
                assertThat(response.isSuccessful()).isTrue();
                assertThat(response.hasApprovalKey()).isTrue();
                return true;
            })
            .verifyComplete();
        
        // 3. 요청 검증
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }
    
    @Test
    void 토큰_캐싱_동작_확인() throws Exception {
        // Given
        KisTokenResponse tokenResponse = new KisTokenResponse(
            "cached-token",
            "Bearer",
            86400L,
            "2024-12-31 23:59:59"
        );
        
        MockResponse tokenMockResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(tokenResponse));
        
        mockWebServer.enqueue(tokenMockResponse);
        
        // When & Then
        // 첫 번째 토큰 요청
        Mono<KisTokenResponse> firstResult = kisTokenService.getAccessToken();
        StepVerifier.create(firstResult)
            .expectNextMatches(response -> response.accessToken().equals("cached-token"))
            .verifyComplete();
        
        // 두 번째 토큰 요청 (캐시 사용)
        Mono<KisTokenResponse> secondResult = kisTokenService.getAccessToken();
        StepVerifier.create(secondResult)
            .expectNextMatches(response -> response.accessToken().equals("cached-token"))
            .verifyComplete();
        
        // 세 번째 토큰 요청 (캐시 사용)
        Mono<KisTokenResponse> thirdResult = kisTokenService.getAccessToken();
        StepVerifier.create(thirdResult)
            .expectNextMatches(response -> response.accessToken().equals("cached-token"))
            .verifyComplete();
        
        // HTTP 요청은 한 번만 발생해야 함
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        assertThat(kisTokenService.hasValidToken()).isTrue();
    }
    
    @Test
    void 에러_발생_시_재시도_동작_확인() throws Exception {
        // Given
        // 첫 번째, 두 번째 요청은 실패
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        
        // 세 번째 요청은 성공
        KisWebSocketKeyResponse successResponse = new KisWebSocketKeyResponse(
            "retry-success-key",
            "0",
            "재시도 성공"
        );
        
        MockResponse successMockResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(successResponse));
        
        mockWebServer.enqueue(successMockResponse);
        
        // When & Then
        Mono<KisWebSocketKeyResponse> result = kisWebSocketKeyService.getWebSocketApprovalKey();
        
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                assertThat(response.approvalKey()).isEqualTo("retry-success-key");
                assertThat(response.isSuccessful()).isTrue();
                return true;
            })
            .verifyComplete();
        
        // 3번의 요청이 발생해야 함 (2번 실패 + 1번 성공)
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }
    
    @Test
    void 토큰_무효화_후_새_토큰_발급() throws Exception {
        // Given
        KisTokenResponse firstToken = new KisTokenResponse(
            "first-token",
            "Bearer",
            86400L,
            "2024-12-31 23:59:59"
        );
        
        KisTokenResponse secondToken = new KisTokenResponse(
            "second-token",
            "Bearer",
            86400L,
            "2024-12-31 23:59:59"
        );
        
        // 첫 번째 토큰 발급
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(firstToken)));
        
        // 토큰 무효화
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        
        // 두 번째 토큰 발급
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(secondToken)));
        
        // When & Then
        // 1. 첫 번째 토큰 발급
        Mono<KisTokenResponse> firstResult = kisTokenService.getAccessToken();
        StepVerifier.create(firstResult)
            .expectNextMatches(response -> response.accessToken().equals("first-token"))
            .verifyComplete();
        
        assertThat(kisTokenService.hasValidToken()).isTrue();
        
        // 2. 토큰 무효화
        Mono<Void> revokeResult = kisTokenService.revokeToken("first-token");
        StepVerifier.create(revokeResult)
            .verifyComplete();
        
        assertThat(kisTokenService.hasValidToken()).isFalse();
        
        // 3. 새 토큰 발급
        Mono<KisTokenResponse> secondResult = kisTokenService.getAccessToken();
        StepVerifier.create(secondResult)
            .expectNextMatches(response -> response.accessToken().equals("second-token"))
            .verifyComplete();
        
        assertThat(kisTokenService.hasValidToken()).isTrue();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }
}