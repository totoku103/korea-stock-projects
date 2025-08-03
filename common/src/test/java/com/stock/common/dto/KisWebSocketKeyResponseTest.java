package com.stock.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KisWebSocketKeyResponseTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    @Test
    void 생성자_테스트_모든_파라미터() {
        // Given
        String approvalKey = "test-approval-key-12345";
        String messageCode = "0";
        String message = "정상처리 되었습니다.";
        LocalDateTime issuedAt = LocalDateTime.now();
        
        // When
        KisWebSocketKeyResponse response = new KisWebSocketKeyResponse(
            approvalKey, messageCode, message, issuedAt
        );
        
        // Then
        assertThat(response.approvalKey()).isEqualTo(approvalKey);
        assertThat(response.messageCode()).isEqualTo(messageCode);
        assertThat(response.message()).isEqualTo(message);
        assertThat(response.issuedAt()).isEqualTo(issuedAt);
    }
    
    @Test
    void 생성자_테스트_기본_생성자() {
        // Given
        String approvalKey = "test-approval-key-12345";
        String messageCode = "0";
        String message = "정상처리 되었습니다.";
        LocalDateTime beforeCreation = LocalDateTime.now();
        
        // When
        KisWebSocketKeyResponse response = new KisWebSocketKeyResponse(
            approvalKey, messageCode, message
        );
        
        LocalDateTime afterCreation = LocalDateTime.now();
        
        // Then
        assertThat(response.approvalKey()).isEqualTo(approvalKey);
        assertThat(response.messageCode()).isEqualTo(messageCode);
        assertThat(response.message()).isEqualTo(message);
        assertThat(response.issuedAt()).isBetween(beforeCreation, afterCreation);
    }
    
    @Test
    void isSuccessful_성공_케이스() {
        // Given
        KisWebSocketKeyResponse response = new KisWebSocketKeyResponse(
            "test-key", "0", "정상처리"
        );
        
        // When & Then
        assertThat(response.isSuccessful()).isTrue();
    }
    
    @Test
    void isSuccessful_실패_케이스() {
        // Given
        KisWebSocketKeyResponse response = new KisWebSocketKeyResponse(
            null, "40310000", "모의투자 미신청계좌입니다."
        );
        
        // When & Then
        assertThat(response.isSuccessful()).isFalse();
    }
    
    @Test
    void hasApprovalKey_유효한_키_있음() {
        // Given
        KisWebSocketKeyResponse response = new KisWebSocketKeyResponse(
            "test-approval-key", "0", "정상처리"
        );
        
        // When & Then
        assertThat(response.hasApprovalKey()).isTrue();
    }
    
    @Test
    void hasApprovalKey_키_없음() {
        // Given
        KisWebSocketKeyResponse response = new KisWebSocketKeyResponse(
            null, "40310000", "에러 메시지"
        );
        
        // When & Then
        assertThat(response.hasApprovalKey()).isFalse();
    }
    
    @Test
    void hasApprovalKey_빈_키() {
        // Given
        KisWebSocketKeyResponse response = new KisWebSocketKeyResponse(
            "", "0", "정상처리"
        );
        
        // When & Then
        assertThat(response.hasApprovalKey()).isFalse();
    }
    
    @Test
    void hasApprovalKey_공백_키() {
        // Given
        KisWebSocketKeyResponse response = new KisWebSocketKeyResponse(
            "   ", "0", "정상처리"
        );
        
        // When & Then
        assertThat(response.hasApprovalKey()).isFalse();
    }
    
    @Test
    void JSON_직렬화_테스트() throws Exception {
        // Given
        KisWebSocketKeyResponse response = new KisWebSocketKeyResponse(
            "test-approval-key", "0", "정상처리"
        );
        
        // When
        String json = objectMapper.writeValueAsString(response);
        
        // Then
        assertThat(json).contains("\"approval_key\":\"test-approval-key\"");
        assertThat(json).contains("\"msg_cd\":\"0\"");
        assertThat(json).contains("\"msg1\":\"정상처리\"");
    }
    
    @Test
    void JSON_역직렬화_테스트() throws Exception {
        // Given
        String json = """
            {
                "approval_key": "test-approval-key-12345",
                "msg_cd": "0",
                "msg1": "정상처리 되었습니다."
            }
            """;
        
        // When
        KisWebSocketKeyResponse response = objectMapper.readValue(json, KisWebSocketKeyResponse.class);
        
        // Then
        assertThat(response.approvalKey()).isEqualTo("test-approval-key-12345");
        assertThat(response.messageCode()).isEqualTo("0");
        assertThat(response.message()).isEqualTo("정상처리 되었습니다.");
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.hasApprovalKey()).isTrue();
    }
}