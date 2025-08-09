package com.stock.common.service;

import com.stock.common.dto.KisWebSocketKeyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 한국투자증권 API Mock 서비스
 * 실제 API 키가 없거나 개발/테스트 환경에서 사용
 */
@Service
@ConditionalOnProperty(name = "kis.api.mock.enabled", havingValue = "true", matchIfMissing = false)
public class KisMockService {
    
    private static final Logger log = LoggerFactory.getLogger(KisMockService.class);
    
    /**
     * Mock 웹소켓 접속키 응답 생성
     */
    public Mono<KisWebSocketKeyResponse> getMockWebSocketApprovalKey() {
        log.info("Mock 웹소켓 접속키 발급");
        
        KisWebSocketKeyResponse mockResponse = new KisWebSocketKeyResponse(
            "mock-approval-key-" + System.currentTimeMillis(),
            "0000", 
            "SUCCESS"
        );
        
        return Mono.just(mockResponse);
    }
    
    /**
     * API 키 유효성 간단 검증
     */
    public boolean isValidApiKey(String appKey, String appSecret) {
        // Mock 환경에서는 기본 테스트 키들을 허용
        if ("test-app-key".equals(appKey) && "test-app-secret".equals(appSecret)) {
            return true;
        }
        
        // 실제 API 키 형식 검증 (한국투자증권 API 키는 보통 특정 패턴을 가짐)
        return appKey != null && appKey.length() > 10 && 
               appSecret != null && appSecret.length() > 10 &&
               !appKey.startsWith("test") && !appSecret.startsWith("test");
    }
    
    /**
     * 403 오류 발생 시 가능한 해결책 제안
     */
    public String getSuggestionFor403Error() {
        return """
            403 Forbidden 오류 해결 방법:
            
            1. API 키 확인
               - KIS_API_APP_KEY가 올바르게 설정되었는지 확인
               - KIS_API_APP_SECRET이 올바르게 설정되었는지 확인
               
            2. 환경 설정 확인
               - 모의투자: https://openapivts.koreainvestment.com:29443
               - 실투자: https://openapi.koreainvestment.com:9443
               
            3. API 권한 확인
               - 한국투자증권 포털에서 API 키 권한 설정 확인
               - 웹소켓 권한이 활성화되어 있는지 확인
               
            4. 네트워크 확인
               - 방화벽이나 프록시 설정 확인
               - VPN 사용 시 IP 제한 확인
               
            5. Mock 환경 사용 (개발용)
               - kis.api.mock.enabled=true 설정으로 Mock 서비스 사용
            """;
    }
}