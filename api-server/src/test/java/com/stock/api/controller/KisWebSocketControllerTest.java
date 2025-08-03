package com.stock.api.controller;

import com.stock.common.dto.KisWebSocketKeyResponse;
import com.stock.common.exception.KisApiException;
import com.stock.common.service.KisWebSocketKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = KisWebSocketController.class,
             excludeAutoConfiguration = org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class)
class KisWebSocketControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private KisWebSocketKeyService kisWebSocketKeyService;
    
    private KisWebSocketKeyResponse mockSuccessResponse;
    private KisWebSocketKeyResponse mockFailureResponse;
    
    @BeforeEach
    void setUp() {
        mockSuccessResponse = new KisWebSocketKeyResponse(
            "test-approval-key-12345",
            "0",
            "정상처리 되었습니다."
        );
        
        mockFailureResponse = new KisWebSocketKeyResponse(
            null,
            "40310000",
            "모의투자 미신청계좌입니다."
        );
    }
    
    @Test
    void getApprovalKey_성공() {
        // Given
        when(kisWebSocketKeyService.getWebSocketApprovalKey())
            .thenReturn(Mono.just(mockSuccessResponse));
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/kis/websocket/approval-key")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("웹소켓 접속키 발급 성공")
            .jsonPath("$.data.approval_key").isEqualTo("test-approval-key-12345")
            .jsonPath("$.data.msg_cd").isEqualTo("0")
            .jsonPath("$.data.msg1").isEqualTo("정상처리 되었습니다.");
    }
    
    @Test
    void getApprovalKey_API_응답_실패() {
        // Given
        when(kisWebSocketKeyService.getWebSocketApprovalKey())
            .thenReturn(Mono.just(mockFailureResponse));
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/kis/websocket/approval-key")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.message").value(message -> 
                ((String) message).contains("웹소켓 접속키 발급 실패"))
            .jsonPath("$.error_code").isEqualTo("40310000");
    }
    
    @Test
    void getApprovalKey_네트워크_오류() {
        // Given
        when(kisWebSocketKeyService.getWebSocketApprovalKey())
            .thenReturn(Mono.error(new KisApiException("네트워크 오류")));
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/kis/websocket/approval-key")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.message").value(message -> 
                ((String) message).contains("웹소켓 접속키 발급 실패"));
    }
    
    @Test
    void getApprovalKey_빈_응답키() {
        // Given
        KisWebSocketKeyResponse emptyKeyResponse = new KisWebSocketKeyResponse(
            "",
            "0",
            "정상처리 되었습니다."
        );
        
        when(kisWebSocketKeyService.getWebSocketApprovalKey())
            .thenReturn(Mono.just(emptyKeyResponse));
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/kis/websocket/approval-key")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.approval_key").isEqualTo("")
            .jsonPath("$.data.msg_cd").isEqualTo("0");
    }
    
    @Test
    void getApprovalKey_null_응답키() {
        // Given
        KisWebSocketKeyResponse nullKeyResponse = new KisWebSocketKeyResponse(
            null,
            "0",
            "정상처리 되었습니다."
        );
        
        when(kisWebSocketKeyService.getWebSocketApprovalKey())
            .thenReturn(Mono.just(nullKeyResponse));
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/kis/websocket/approval-key")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.approval_key").isEmpty()
            .jsonPath("$.data.msg_cd").isEqualTo("0");
    }
}