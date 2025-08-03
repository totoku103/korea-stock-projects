package com.stock.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KisWebSocketKeyRequestTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    void 생성자_테스트() {
        // Given
        String grantType = "client_credentials";
        String appKey = "test-app-key";
        String secretKey = "test-secret-key";
        
        // When
        KisWebSocketKeyRequest request = new KisWebSocketKeyRequest(grantType, appKey, secretKey);
        
        // Then
        assertThat(request.grantType()).isEqualTo(grantType);
        assertThat(request.appKey()).isEqualTo(appKey);
        assertThat(request.secretKey()).isEqualTo(secretKey);
    }
    
    @Test
    void of_팩토리_메소드_테스트() {
        // Given
        String appKey = "test-app-key";
        String secretKey = "test-secret-key";
        
        // When
        KisWebSocketKeyRequest request = KisWebSocketKeyRequest.of(appKey, secretKey);
        
        // Then
        assertThat(request.grantType()).isEqualTo("client_credentials");
        assertThat(request.appKey()).isEqualTo(appKey);
        assertThat(request.secretKey()).isEqualTo(secretKey);
    }
    
    @Test
    void JSON_직렬화_테스트() throws Exception {
        // Given
        KisWebSocketKeyRequest request = KisWebSocketKeyRequest.of("test-app-key", "test-secret-key");
        
        // When
        String json = objectMapper.writeValueAsString(request);
        
        // Then
        assertThat(json).contains("\"grant_type\":\"client_credentials\"");
        assertThat(json).contains("\"appkey\":\"test-app-key\"");
        assertThat(json).contains("\"secretkey\":\"test-secret-key\"");
    }
    
    @Test
    void JSON_역직렬화_테스트() throws Exception {
        // Given
        String json = """
            {
                "grant_type": "client_credentials",
                "appkey": "test-app-key",
                "secretkey": "test-secret-key"
            }
            """;
        
        // When
        KisWebSocketKeyRequest request = objectMapper.readValue(json, KisWebSocketKeyRequest.class);
        
        // Then
        assertThat(request.grantType()).isEqualTo("client_credentials");
        assertThat(request.appKey()).isEqualTo("test-app-key");
        assertThat(request.secretKey()).isEqualTo("test-secret-key");
    }
}