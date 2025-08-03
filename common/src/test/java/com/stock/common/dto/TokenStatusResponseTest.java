package com.stock.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TokenStatusResponseTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    @Test
    void fromTokenResponse_유효한_토큰() {
        // Given - 현재 시간을 기준으로 유효한 토큰 생성
        LocalDateTime now = LocalDateTime.now();
        KisTokenResponse tokenResponse = new KisTokenResponse(
            "test-token", "Bearer", 3600L, "2024-12-31 23:59:59", now
        );
        
        // When
        TokenStatusResponse response = TokenStatusResponse.fromTokenResponse(tokenResponse);
        
        // Then
        assertThat(response.hasValidToken()).isTrue();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresAt()).isEqualTo(now.plusSeconds(3600L));
        assertThat(response.issuedAt()).isEqualTo(now);
        assertThat(response.expiresInSeconds()).isEqualTo(3600L);
    }
    
    @Test
    void fromTokenResponse_만료된_토큰() {
        // Given - 2초 전에 발급되고 1초 후 만료되는 토큰 (이미 만료됨)
        LocalDateTime pastTime = LocalDateTime.now().minusSeconds(2);
        KisTokenResponse tokenResponse = new KisTokenResponse(
            "expired-token", "Bearer", 1L, "2024-01-01 13:00:00", pastTime
        );
        
        // When
        TokenStatusResponse response = TokenStatusResponse.fromTokenResponse(tokenResponse);
        
        // Then
        assertThat(response.hasValidToken()).isFalse();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(1L);
    }
    
    @Test
    void fromTokenResponse_null_토큰() {
        // When
        TokenStatusResponse response = TokenStatusResponse.fromTokenResponse(null);
        
        // Then
        assertThat(response.hasValidToken()).isFalse();
        assertThat(response.tokenType()).isNull();
        assertThat(response.expiresAt()).isNull();
        assertThat(response.issuedAt()).isNull();
        assertThat(response.expiresInSeconds()).isEqualTo(0L);
    }
    
    @Test
    void noToken_정적_메소드() {
        // When
        TokenStatusResponse response = TokenStatusResponse.noToken();
        
        // Then
        assertThat(response.hasValidToken()).isFalse();
        assertThat(response.tokenType()).isNull();
        assertThat(response.expiresAt()).isNull();
        assertThat(response.issuedAt()).isNull();
        assertThat(response.expiresInSeconds()).isEqualTo(0L);
    }
    
    @Test
    void JSON_직렬화_테스트() throws Exception {
        // Given
        LocalDateTime issuedAt = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        LocalDateTime expiresAt = LocalDateTime.of(2024, 1, 1, 13, 0, 0);
        TokenStatusResponse response = new TokenStatusResponse(
            true, "Bearer", expiresAt, issuedAt, 3600L
        );
        
        // When
        String json = objectMapper.writeValueAsString(response);
        
        // Then
        assertThat(json).contains("\"has_valid_token\":true");
        assertThat(json).contains("\"token_type\":\"Bearer\"");
        assertThat(json).contains("\"expires_in_seconds\":3600");
    }
    
    @Test
    void JSON_역직렬화_테스트() throws Exception {
        // Given
        String json = """
            {
                "has_valid_token": true,
                "token_type": "Bearer",
                "expires_at": "2024-01-01T13:00:00",
                "issued_at": "2024-01-01T12:00:00",
                "expires_in_seconds": 3600
            }
            """;
        
        // When
        TokenStatusResponse response = objectMapper.readValue(json, TokenStatusResponse.class);
        
        // Then
        assertThat(response.hasValidToken()).isTrue();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 13, 0, 0));
        assertThat(response.issuedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 12, 0, 0));
        assertThat(response.expiresInSeconds()).isEqualTo(3600L);
    }
    
    @Test
    void JSON_직렬화_noToken_응답() throws Exception {
        // Given
        TokenStatusResponse response = TokenStatusResponse.noToken();
        
        // When
        String json = objectMapper.writeValueAsString(response);
        
        // Then
        assertThat(json).contains("\"has_valid_token\":false");
        assertThat(json).contains("\"token_type\":null");
        assertThat(json).contains("\"expires_at\":null");
        assertThat(json).contains("\"issued_at\":null");
        assertThat(json).contains("\"expires_in_seconds\":0");
    }
}