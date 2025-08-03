package com.stock.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    @Test
    void success_데이터만_있는_경우() {
        // Given
        String testData = "test data";
        
        // When
        ApiResponse<String> response = ApiResponse.success(testData);
        
        // Then
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("성공");
        assertThat(response.data()).isEqualTo(testData);
        assertThat(response.errorCode()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }
    
    @Test
    void success_메시지와_데이터가_있는_경우() {
        // Given
        String message = "작업 완료";
        String testData = "result data";
        
        // When
        ApiResponse<String> response = ApiResponse.success(message, testData);
        
        // Then
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo(message);
        assertThat(response.data()).isEqualTo(testData);
        assertThat(response.errorCode()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }
    
    @Test
    void error_메시지만_있는_경우() {
        // Given
        String errorMessage = "오류 발생";
        
        // When
        ApiResponse<String> response = ApiResponse.error(errorMessage);
        
        // Then
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo(errorMessage);
        assertThat(response.data()).isNull();
        assertThat(response.errorCode()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }
    
    @Test
    void error_메시지와_에러코드가_있는_경우() {
        // Given
        String errorMessage = "API 호출 실패";
        String errorCode = "E001";
        
        // When
        ApiResponse<String> response = ApiResponse.error(errorMessage, errorCode);
        
        // Then
        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo(errorMessage);
        assertThat(response.data()).isNull();
        assertThat(response.errorCode()).isEqualTo(errorCode);
        assertThat(response.timestamp()).isNotNull();
    }
    
    @Test
    void JSON_직렬화_테스트() throws Exception {
        // Given
        ApiResponse<String> response = ApiResponse.success("테스트 성공", "test data");
        
        // When
        String json = objectMapper.writeValueAsString(response);
        
        // Then
        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"message\":\"테스트 성공\"");
        assertThat(json).contains("\"data\":\"test data\"");
        assertThat(json).contains("\"timestamp\":");
    }
    
    @Test
    void JSON_역직렬화_테스트() throws Exception {
        // Given
        String json = """
            {
                "success": true,
                "message": "성공",
                "data": "test data",
                "error_code": null,
                "timestamp": "2024-01-01T12:00:00"
            }
            """;
        
        // When
        @SuppressWarnings("unchecked")
        ApiResponse<String> response = objectMapper.readValue(json, ApiResponse.class);
        
        // Then
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("성공");
        assertThat(response.data()).isEqualTo("test data");
        assertThat(response.errorCode()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }
    
    @Test
    void 복잡한_객체_데이터_테스트() {
        // Given
        KisTokenResponse tokenResponse = new KisTokenResponse(
            "test-token", "Bearer", 3600L, "2024-12-31 23:59:59"
        );
        
        // When
        ApiResponse<KisTokenResponse> response = ApiResponse.success("토큰 발급 성공", tokenResponse);
        
        // Then
        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("토큰 발급 성공");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().accessToken()).isEqualTo("test-token");
        assertThat(response.data().tokenType()).isEqualTo("Bearer");
    }
}