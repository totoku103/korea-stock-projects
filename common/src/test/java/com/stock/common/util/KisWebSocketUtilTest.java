package com.stock.common.util;

import com.stock.common.config.KisApiProperties;
import com.stock.common.constants.KisApiConstants;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KisWebSocketUtilTest {
    
    @Test
    void getWebSocketUrl_실운영_환경() {
        // Given
        KisApiProperties realProperties = new KisApiProperties(
            "test-app-key",
            "test-app-secret",
            "01234567-89",
            "real",
            "https://openapi.koreainvestment.com:9443",
            "ws://ops.koreainvestment.com:21000",
            new KisApiProperties.RateLimit(20, 10000, 5),
            new KisApiProperties.Timeout(5000, 30000, 30000)
        );
        
        // When
        String webSocketUrl = KisWebSocketUtil.getWebSocketUrl(realProperties);
        
        // Then
        assertThat(webSocketUrl).isEqualTo(KisApiConstants.WS_REAL_URL);
    }
    
    @Test
    void getWebSocketUrl_모의투자_환경() {
        // Given
        KisApiProperties mockProperties = new KisApiProperties(
            "test-app-key",
            "test-app-secret",
            "01234567-89",
            "mock",
            "https://openapivts.koreainvestment.com:29443",
            "ws://ops.koreainvestment.com:31000",
            new KisApiProperties.RateLimit(20, 10000, 5),
            new KisApiProperties.Timeout(5000, 30000, 30000)
        );
        
        // When
        String webSocketUrl = KisWebSocketUtil.getWebSocketUrl(mockProperties);
        
        // Then
        assertThat(webSocketUrl).isEqualTo(KisApiConstants.WS_MOCK_URL);
    }
    
    @Test
    void createWebSocketHeaders_개인고객() {
        // Given
        String approvalKey = "test-approval-key-12345";
        String custtype = "P";
        
        // When
        Map<String, String> headers = KisWebSocketUtil.createWebSocketHeaders(approvalKey, custtype);
        
        // Then
        assertThat(headers).containsEntry("approval_key", approvalKey);
        assertThat(headers).containsEntry("custtype", custtype);
        assertThat(headers).hasSize(2);
    }
    
    @Test
    void createWebSocketHeaders_법인고객() {
        // Given
        String approvalKey = "test-approval-key-67890";
        String custtype = "B";
        
        // When
        Map<String, String> headers = KisWebSocketUtil.createWebSocketHeaders(approvalKey, custtype);
        
        // Then
        assertThat(headers).containsEntry("approval_key", approvalKey);
        assertThat(headers).containsEntry("custtype", custtype);
        assertThat(headers).hasSize(2);
    }
    
    @Test
    void createStockPriceSubscriptionMessage_등록() {
        // Given
        String stockCode = "005930"; // 삼성전자
        String trType = "1"; // 등록
        
        // When
        String message = KisWebSocketUtil.createStockPriceSubscriptionMessage(stockCode, trType);
        
        // Then
        assertThat(message).contains("\"tr_type\":\"1\"");
        assertThat(message).contains("\"tr_id\":\"H0STCNT0\"");
        assertThat(message).contains("\"tr_key\":\"005930\"");
        assertThat(message).contains("\"custtype\":\"P\"");
        assertThat(message).contains("\"approval_key\":\"\""); // 빈 문자열로 설정됨
    }
    
    @Test
    void createStockPriceSubscriptionMessage_해제() {
        // Given
        String stockCode = "000660"; // SK하이닉스
        String trType = "2"; // 해제
        
        // When
        String message = KisWebSocketUtil.createStockPriceSubscriptionMessage(stockCode, trType);
        
        // Then
        assertThat(message).contains("\"tr_type\":\"2\"");
        assertThat(message).contains("\"tr_id\":\"H0STCNT0\"");
        assertThat(message).contains("\"tr_key\":\"000660\"");
        assertThat(message).contains("\"custtype\":\"P\"");
    }
    
    @Test
    void createOrderbookSubscriptionMessage_등록() {
        // Given
        String stockCode = "035420"; // NAVER
        String trType = "1"; // 등록
        
        // When
        String message = KisWebSocketUtil.createOrderbookSubscriptionMessage(stockCode, trType);
        
        // Then
        assertThat(message).contains("\"tr_type\":\"1\"");
        assertThat(message).contains("\"tr_id\":\"H0STASP0\"");
        assertThat(message).contains("\"tr_key\":\"035420\"");
        assertThat(message).contains("\"custtype\":\"P\"");
        assertThat(message).contains("\"approval_key\":\"\""); // 빈 문자열로 설정됨
    }
    
    @Test
    void createOrderbookSubscriptionMessage_해제() {
        // Given
        String stockCode = "207940"; // 삼성바이오로직스
        String trType = "2"; // 해제
        
        // When
        String message = KisWebSocketUtil.createOrderbookSubscriptionMessage(stockCode, trType);
        
        // Then
        assertThat(message).contains("\"tr_type\":\"2\"");
        assertThat(message).contains("\"tr_id\":\"H0STASP0\"");
        assertThat(message).contains("\"tr_key\":\"207940\"");
        assertThat(message).contains("\"custtype\":\"P\"");
    }
    
    @Test
    void createStockPriceSubscriptionMessage_JSON_유효성_검증() {
        // Given
        String stockCode = "005930";
        String trType = "1";
        
        // When
        String message = KisWebSocketUtil.createStockPriceSubscriptionMessage(stockCode, trType);
        
        // Then
        // JSON 형태로 잘 구성되었는지 확인
        assertThat(message).startsWith("{");
        assertThat(message).endsWith("}");
        assertThat(message).contains("\"header\":");
        assertThat(message).contains("\"body\":");
        assertThat(message).contains("\"input\":");
    }
    
    @Test
    void createOrderbookSubscriptionMessage_JSON_유효성_검증() {
        // Given
        String stockCode = "000660";
        String trType = "1";
        
        // When
        String message = KisWebSocketUtil.createOrderbookSubscriptionMessage(stockCode, trType);
        
        // Then
        // JSON 형태로 잘 구성되었는지 확인
        assertThat(message).startsWith("{");
        assertThat(message).endsWith("}");
        assertThat(message).contains("\"header\":");
        assertThat(message).contains("\"body\":");
        assertThat(message).contains("\"input\":");
    }
}