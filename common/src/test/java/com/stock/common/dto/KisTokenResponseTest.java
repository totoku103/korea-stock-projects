package com.stock.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KisTokenResponseTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    @Test
    void 생성자_테스트_모든_파라미터() {
        // Given
        String accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...";
        String tokenType = "Bearer";
        long expiresIn = 86400L;
        String accessTokenExpired = "2024-12-31 23:59:59";
        LocalDateTime issuedAt = LocalDateTime.now();
        
        // When
        KisTokenResponse response = new KisTokenResponse(
            accessToken, tokenType, expiresIn, accessTokenExpired, issuedAt
        );
        
        // Then
        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.tokenType()).isEqualTo(tokenType);
        assertThat(response.expiresIn()).isEqualTo(expiresIn);
        assertThat(response.accessTokenExpired()).isEqualTo(accessTokenExpired);
        assertThat(response.issuedAt()).isEqualTo(issuedAt);
    }
    
    @Test
    void 생성자_테스트_기본_생성자() {
        // Given
        String accessToken = "test-token";
        String tokenType = "Bearer";
        long expiresIn = 3600L;
        String accessTokenExpired = "2024-12-31 23:59:59";
        LocalDateTime beforeCreation = LocalDateTime.now();
        
        // When
        KisTokenResponse response = new KisTokenResponse(
            accessToken, tokenType, expiresIn, accessTokenExpired
        );
        
        LocalDateTime afterCreation = LocalDateTime.now();
        
        // Then
        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.tokenType()).isEqualTo(tokenType);
        assertThat(response.expiresIn()).isEqualTo(expiresIn);
        assertThat(response.accessTokenExpired()).isEqualTo(accessTokenExpired);
        assertThat(response.issuedAt()).isBetween(beforeCreation, afterCreation);
    }
    
    @Test
    void isExpired_유효한_토큰() {
        // Given
        KisTokenResponse response = new KisTokenResponse(
            "test-token", "Bearer", 3600L, "2024-12-31 23:59:59"
        );
        
        // When & Then
        assertThat(response.isExpired()).isFalse();
    }
    
    @Test
    void isExpired_만료된_토큰() {
        // Given - 2초 전에 발급되고 1초 후 만료되는 토큰 (이미 만료됨)
        LocalDateTime pastTime = LocalDateTime.now().minusSeconds(2);
        KisTokenResponse response = new KisTokenResponse(
            "expired-token", "Bearer", 1L, "2024-12-31 23:59:59", pastTime
        );
        
        // When & Then
        assertThat(response.isExpired()).isTrue();
    }
    
    @Test
    void getExpiresAt_만료_시간_계산() {
        // Given
        LocalDateTime issuedAt = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        long expiresIn = 3600L; // 1시간
        
        KisTokenResponse response = new KisTokenResponse(
            "test-token", "Bearer", expiresIn, "2024-01-01 13:00:00", issuedAt
        );
        
        // When
        LocalDateTime expiresAt = response.getExpiresAt();
        
        // Then
        LocalDateTime expectedExpiresAt = LocalDateTime.of(2024, 1, 1, 13, 0, 0);
        assertThat(expiresAt).isEqualTo(expectedExpiresAt);
    }
    
    @Test
    void getBearerToken_Bearer_토큰_생성() {
        // Given
        String accessToken = "test-access-token";
        KisTokenResponse response = new KisTokenResponse(
            accessToken, "Bearer", 3600L, "2024-12-31 23:59:59"
        );
        
        // When
        String bearerToken = response.getBearerToken();
        
        // Then
        assertThat(bearerToken).isEqualTo("Bearer test-access-token");
    }
    
    @Test
    void JSON_직렬화_테스트() throws Exception {
        // Given
        KisTokenResponse response = new KisTokenResponse(
            "test-token", "Bearer", 3600L, "2024-12-31 23:59:59"
        );
        
        // When
        String json = objectMapper.writeValueAsString(response);
        
        // Then
        assertThat(json).contains("\"access_token\":\"test-token\"");
        assertThat(json).contains("\"token_type\":\"Bearer\"");
        assertThat(json).contains("\"expires_in\":3600");
        assertThat(json).contains("\"access_token_token_expired\":\"2024-12-31 23:59:59\"");
    }
    
    @Test
    void JSON_역직렬화_테스트() throws Exception {
        // Given
        String json = """
            {
                "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
                "token_type": "Bearer",
                "expires_in": 86400,
                "access_token_token_expired": "2024-12-31 23:59:59"
            }
            """;
        
        // When
        KisTokenResponse response = objectMapper.readValue(json, KisTokenResponse.class);
        
        // Then
        assertThat(response.accessToken()).isEqualTo("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(86400L);
        assertThat(response.accessTokenExpired()).isEqualTo("2024-12-31 23:59:59");
        assertThat(response.getBearerToken()).isEqualTo("Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...");
        // issuedAt은 JSON에 포함되지 않으므로 null이 될 수 있음 - 이는 정상 동작
    }
}