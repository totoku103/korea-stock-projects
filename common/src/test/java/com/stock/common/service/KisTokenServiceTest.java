package com.stock.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.common.config.KisApiProperties;
import com.stock.common.dto.KisTokenResponse;
import com.stock.common.exception.KisApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KisTokenServiceTest {
    
    private MockWebServer mockWebServer;
    private KisTokenService kisTokenService;
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
        objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void getAccessToken_새로운_토큰_발급() throws JsonProcessingException, InterruptedException {
        // Given
        KisTokenResponse expectedResponse = new KisTokenResponse(
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
            "Bearer",
            86400L,
            "2024-12-31 23:59:59"
        );
        
        MockResponse mockResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(expectedResponse));
        
        mockWebServer.enqueue(mockResponse);
        
        // When & Then
        Mono<KisTokenResponse> result = kisTokenService.getAccessToken();
        
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                assertThat(response.accessToken()).isEqualTo("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...");
                assertThat(response.tokenType()).isEqualTo("Bearer");
                assertThat(response.expiresIn()).isEqualTo(86400L);
                assertThat(response.isExpired()).isFalse();
                assertThat(response.getBearerToken()).isEqualTo("Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...");
                return true;
            })
            .verifyComplete();
        
        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/oauth2/tokenP");
        assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json; charset=utf-8");
    }
    
    @Test
    void getAccessToken_캐시된_토큰_재사용() throws JsonProcessingException {
        // Given
        KisTokenResponse cachedToken = new KisTokenResponse(
            "cached-token",
            "Bearer",
            86400L,
            "2024-12-31 23:59:59",
            LocalDateTime.now()
        );
        
        // 첫 번째 호출로 토큰을 캐시에 저장
        MockResponse mockResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(cachedToken));
        
        mockWebServer.enqueue(mockResponse);
        
        // When & Then
        // 첫 번째 호출
        Mono<KisTokenResponse> firstResult = kisTokenService.getAccessToken();
        StepVerifier.create(firstResult)
            .expectNextMatches(response -> response.accessToken().equals("cached-token"))
            .verifyComplete();
        
        // 두 번째 호출 (캐시된 토큰 사용, 새로운 HTTP 요청 없음)
        Mono<KisTokenResponse> secondResult = kisTokenService.getAccessToken();
        StepVerifier.create(secondResult)
            .expectNextMatches(response -> response.accessToken().equals("cached-token"))
            .verifyComplete();
        
        // MockWebServer에는 한 번만 요청이 들어와야 함
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }
    
    @Test
    void getAccessToken_만료된_토큰_갱신() throws JsonProcessingException {
        // Given
        // 만료된 토큰으로 첫 번째 응답 설정
        KisTokenResponse expiredToken = new KisTokenResponse(
            "expired-token",
            "Bearer",
            1L, // 1초 후 만료
            "2024-12-31 23:59:59",
            LocalDateTime.now().minusSeconds(2) // 2초 전에 발급되어 이미 만료됨
        );
        
        // 새로운 토큰으로 두 번째 응답 설정
        KisTokenResponse newToken = new KisTokenResponse(
            "new-token",
            "Bearer",
            86400L,
            "2024-12-31 23:59:59"
        );
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(expiredToken)));
            
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(newToken)));
        
        // When & Then
        // 첫 번째 호출로 만료된 토큰 캐시
        Mono<KisTokenResponse> firstResult = kisTokenService.getAccessToken();
        StepVerifier.create(firstResult)
            .expectNextMatches(response -> response.accessToken().equals("expired-token"))
            .verifyComplete();
        
        // 두 번째 호출 시 토큰이 만료되어 새로 발급
        Mono<KisTokenResponse> secondResult = kisTokenService.getAccessToken();
        StepVerifier.create(secondResult)
            .expectNextMatches(response -> response.accessToken().equals("new-token"))
            .verifyComplete();
        
        // 두 번의 HTTP 요청이 있어야 함
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }
    
    @Test
    void issueNewAccessToken_강제_새_토큰_발급() throws JsonProcessingException {
        // Given
        KisTokenResponse newToken = new KisTokenResponse(
            "new-forced-token",
            "Bearer",
            86400L,
            "2024-12-31 23:59:59"
        );
        
        MockResponse mockResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(newToken));
        
        mockWebServer.enqueue(mockResponse);
        
        // When & Then
        Mono<KisTokenResponse> result = kisTokenService.issueNewAccessToken();
        
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                assertThat(response.accessToken()).isEqualTo("new-forced-token");
                assertThat(response.tokenType()).isEqualTo("Bearer");
                return true;
            })
            .verifyComplete();
    }
    
    @Test
    void revokeToken_토큰_무효화() throws InterruptedException {
        // Given
        MockResponse mockResponse = new MockResponse()
            .setResponseCode(200);
        
        mockWebServer.enqueue(mockResponse);
        
        // When & Then
        Mono<Void> result = kisTokenService.revokeToken("test-token");
        
        StepVerifier.create(result)
            .verifyComplete();
        
        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/oauth2/revokeP");
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-token");
    }
    
    @Test
    void invalidateCache_캐시_무효화() {
        // Given & When
        kisTokenService.invalidateCache();
        
        // Then
        assertThat(kisTokenService.hasValidToken()).isFalse();
    }
    
    @Test
    void hasValidToken_유효한_토큰_존재_확인() throws JsonProcessingException {
        // Given
        KisTokenResponse validToken = new KisTokenResponse(
            "valid-token",
            "Bearer",
            86400L,
            "2024-12-31 23:59:59"
        );
        
        MockResponse mockResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(validToken));
        
        mockWebServer.enqueue(mockResponse);
        
        // When
        assertThat(kisTokenService.hasValidToken()).isFalse(); // 초기 상태
        
        // 토큰 발급 후
        StepVerifier.create(kisTokenService.getAccessToken())
            .expectNextCount(1)
            .verifyComplete();
        
        // Then
        assertThat(kisTokenService.hasValidToken()).isTrue();
    }
    
    @Test
    void getCachedToken_캐시된_토큰_반환() throws JsonProcessingException {
        // Given
        KisTokenResponse expectedToken = new KisTokenResponse(
            "cached-token",
            "Bearer",
            86400L,
            "2024-12-31 23:59:59"
        );
        
        MockResponse mockResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(expectedToken));
        
        mockWebServer.enqueue(mockResponse);
        
        // When
        assertThat(kisTokenService.getCachedToken()).isNull(); // 초기 상태
        
        // 토큰 발급 후
        StepVerifier.create(kisTokenService.getAccessToken())
            .expectNextCount(1)
            .verifyComplete();
        
        // Then
        KisTokenResponse cachedToken = kisTokenService.getCachedToken();
        assertThat(cachedToken).isNotNull();
        assertThat(cachedToken.accessToken()).isEqualTo("cached-token");
        assertThat(cachedToken.tokenType()).isEqualTo("Bearer");
    }
    
    @Test
    void getCachedToken_캐시_무효화_후_null_반환() throws JsonProcessingException {
        // Given
        KisTokenResponse token = new KisTokenResponse(
            "test-token",
            "Bearer",
            86400L,
            "2024-12-31 23:59:59"
        );
        
        MockResponse mockResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(token));
        
        mockWebServer.enqueue(mockResponse);
        
        // When
        // 토큰 발급
        StepVerifier.create(kisTokenService.getAccessToken())
            .expectNextCount(1)
            .verifyComplete();
        
        assertThat(kisTokenService.getCachedToken()).isNotNull();
        
        // 캐시 무효화
        kisTokenService.invalidateCache();
        
        // Then
        assertThat(kisTokenService.getCachedToken()).isNull();
        assertThat(kisTokenService.hasValidToken()).isFalse();
    }
    
    @Test
    void getAccessToken_네트워크_오류() {
        // Given
        MockResponse mockResponse = new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error");
        
        mockWebServer.enqueue(mockResponse);
        
        // When & Then
        Mono<KisTokenResponse> result = kisTokenService.getAccessToken();
        
        StepVerifier.create(result)
            .expectError(KisApiException.class)
            .verify();
    }
    
    @Test
    void getAccessToken_재시도_성공() throws JsonProcessingException {
        // Given
        // 첫 번째 요청은 실패
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        
        // 두 번째 요청은 성공
        KisTokenResponse validToken = new KisTokenResponse(
            "retry-token",
            "Bearer",
            86400L,
            "2024-12-31 23:59:59"
        );
        
        MockResponse successResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(validToken));
        
        mockWebServer.enqueue(successResponse);
        
        // When & Then
        Mono<KisTokenResponse> result = kisTokenService.getAccessToken();
        
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                assertThat(response.accessToken()).isEqualTo("retry-token");
                return true;
            })
            .expectComplete()
            .verify(Duration.ofSeconds(5));
    }
}