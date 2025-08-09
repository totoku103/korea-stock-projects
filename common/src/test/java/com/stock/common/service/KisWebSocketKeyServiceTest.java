package com.stock.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.common.config.KisApiProperties;
import com.stock.common.dto.KisWebSocketKeyResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

class KisWebSocketKeyServiceTest {
    
    private MockWebServer mockWebServer;
    private KisWebSocketKeyService kisWebSocketKeyService;
    private KisApiProperties kisApiProperties;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        String baseUrl = String.format("http://localhost:%d", mockWebServer.getPort());
        
        kisApiProperties = new KisApiProperties(
            baseUrl,
            "test-app-key",
            "test-app-secret",
            "01234567-89",
            "ws://localhost:31000",
            new KisApiProperties.RateLimit(20, 10000, 5),
            new KisApiProperties.Timeout(5000, 30000, 30000)
        );
        
        WebClient webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
            
        kisWebSocketKeyService = new KisWebSocketKeyService(webClient, kisApiProperties);
        objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void getWebSocketApprovalKey_성공() throws JsonProcessingException, InterruptedException {
        // Given
        KisWebSocketKeyResponse expectedResponse = new KisWebSocketKeyResponse(
            "test-approval-key-12345",
            "0",
            "정상처리 되었습니다."
        );
        
        MockResponse mockResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(expectedResponse));
        
        mockWebServer.enqueue(mockResponse);
        
        // When & Then
        Mono<KisWebSocketKeyResponse> result = kisWebSocketKeyService.getWebSocketApprovalKey();
        
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                assertThat(response.approvalKey()).isEqualTo("test-approval-key-12345");
                assertThat(response.messageCode()).isEqualTo("0");
                assertThat(response.message()).isEqualTo("정상처리 되었습니다.");
                assertThat(response.isSuccessful()).isTrue();
                assertThat(response.hasApprovalKey()).isTrue();
                return true;
            })
            .verifyComplete();
        
        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/oauth2/Approval");
        assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json; charset=utf-8");
    }
    
    @Test
    void getWebSocketApprovalKey_실패_응답() throws JsonProcessingException {
        // Given
        KisWebSocketKeyResponse expectedResponse = new KisWebSocketKeyResponse(
            null,
            "40310000",
            "모의투자 미신청계좌입니다."
        );
        
        MockResponse mockResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(expectedResponse));
        
        mockWebServer.enqueue(mockResponse);
        
        // When & Then
        Mono<KisWebSocketKeyResponse> result = kisWebSocketKeyService.getWebSocketApprovalKey();
        
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                assertThat(response.approvalKey()).isNull();
                assertThat(response.messageCode()).isEqualTo("40310000");
                assertThat(response.message()).isEqualTo("모의투자 미신청계좌입니다.");
                assertThat(response.isSuccessful()).isFalse();
                assertThat(response.hasApprovalKey()).isFalse();
                return true;
            })
            .verifyComplete();
    }
    
    @Test
    void getWebSocketApprovalKey_네트워크_오류() {
        // Given
        MockResponse mockResponse = new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error");
        
        mockWebServer.enqueue(mockResponse);
        
        // When & Then
        Mono<KisWebSocketKeyResponse> result = kisWebSocketKeyService.getWebSocketApprovalKey();
        
        StepVerifier.create(result)
            .expectError(KisApiException.class)
            .verify();
    }
    
    @Test
    void getWebSocketApprovalKey_재시도_성공() throws JsonProcessingException {
        // Given
        // 첫 번째 요청은 실패
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        
        // 두 번째 요청은 성공
        KisWebSocketKeyResponse expectedResponse = new KisWebSocketKeyResponse(
            "test-approval-key-retry",
            "0",
            "정상처리 되었습니다."
        );
        
        MockResponse successResponse = new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsString(expectedResponse));
        
        mockWebServer.enqueue(successResponse);
        
        // When & Then
        Mono<KisWebSocketKeyResponse> result = kisWebSocketKeyService.getWebSocketApprovalKey();
        
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                assertThat(response.approvalKey()).isEqualTo("test-approval-key-retry");
                assertThat(response.messageCode()).isEqualTo("0");
                assertThat(response.isSuccessful()).isTrue();
                return true;
            })
            .expectComplete()
            .verify(Duration.ofSeconds(5));
    }
    
    @Test
    void getWebSocketApprovalKey_타임아웃() {
        // Given
        MockResponse mockResponse = new MockResponse()
            .setResponseCode(200)
            .setBodyDelay(10, java.util.concurrent.TimeUnit.SECONDS); // 10초 지연
        
        mockWebServer.enqueue(mockResponse);
        
        // When & Then
        Mono<KisWebSocketKeyResponse> result = kisWebSocketKeyService.getWebSocketApprovalKey();
        
        StepVerifier.create(result)
            .expectError()
            .verify(Duration.ofSeconds(5));
    }
}