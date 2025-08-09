package com.stock.common.service;

import com.stock.common.config.KisApiProperties;
import com.stock.common.constants.KisApiConstants;
import com.stock.common.dto.KisWebSocketKeyRequest;
import com.stock.common.dto.KisWebSocketKeyResponse;
import com.stock.common.exception.KisApiException;
import com.stock.common.exception.KisApiAuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class KisWebSocketKeyService {
    
    private static final Logger log = LoggerFactory.getLogger(KisWebSocketKeyService.class);
    
    private final WebClient webClient;
    private final KisApiProperties kisApiProperties;
    
    @Autowired(required = false)
    private KisMockService kisMockService;
    
    public KisWebSocketKeyService(WebClient webClient, KisApiProperties kisApiProperties) {
        this.webClient = webClient;
        this.kisApiProperties = kisApiProperties;
    }
    
    /**
     * 웹소켓 실시간 접속키(approval_key)를 발급받습니다.
     * 
     * @return 웹소켓 접속키 응답
     */
    public Mono<KisWebSocketKeyResponse> getWebSocketApprovalKey() {
        log.info("웹소켓 접속키 발급 요청 시작");
        
        // Mock 서비스 사용 조건 확인
        if (kisMockService != null && shouldUseMockService()) {
            log.info("Mock 서비스를 사용하여 웹소켓 접속키 발급");
            return kisMockService.getMockWebSocketApprovalKey();
        }
        
        KisWebSocketKeyRequest request = KisWebSocketKeyRequest.of(
            kisApiProperties.appKey(),
            kisApiProperties.appSecret()
        );
        
        String requestUrl = kisApiProperties.baseUrl() + KisApiConstants.WEBSOCKET_APPROVAL_KEY_ENDPOINT;
        
        log.debug("웹소켓 접속키 요청 정보 - URL: {}, AppKey: {}", 
                requestUrl, maskKey(kisApiProperties.appKey()));
        
        return webClient.post()
                .uri(KisApiConstants.WEBSOCKET_APPROVAL_KEY_ENDPOINT)
                .header(KisApiConstants.HEADER_CONTENT_TYPE, "application/json; charset=utf-8")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                    response -> {
                        log.error("웹소켓 접속키 API 오류 - Status: {}, URL: {}", 
                                response.statusCode(), requestUrl);
                        return response.bodyToMono(String.class)
                            .doOnNext(body -> log.error("API 오류 응답 내용: {}", body))
                            .flatMap(body -> {
                                String errorMessage = createErrorMessage(response.statusCode().value(), body);
                                
                                if (response.statusCode().value() == 401 || response.statusCode().value() == 403) {
                                    // 403 오류 시 추가적인 도움말 제공
                                    if (response.statusCode().value() == 403 && kisMockService != null) {
                                        String suggestions = kisMockService.getSuggestionFor403Error();
                                        log.warn("403 오류 해결 방법:\n{}", suggestions);
                                    }
                                    return Mono.error(new KisApiAuthenticationException(errorMessage));
                                } else {
                                    return Mono.error(new KisApiException(errorMessage));
                                }
                            });
                    })
                .bodyToMono(KisWebSocketKeyResponse.class)
                .doOnNext(response -> {
                    if (response.isSuccessful()) {
                        log.info("웹소켓 접속키 발급 성공: messageCode={}, approvalKey={}", 
                               response.messageCode(), 
                               maskApprovalKey(response.approvalKey()));
                    } else {
                        log.warn("웹소켓 접속키 발급 실패: messageCode={}, message={}", 
                               response.messageCode(), 
                               response.message());
                    }
                })
                .doOnError(error -> log.error("웹소켓 접속키 발급 중 오류 발생", error))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .maxBackoff(Duration.ofSeconds(10))
                    .filter(throwable -> !(throwable instanceof KisApiException)))
                .onErrorMap(throwable -> {
                    if (throwable instanceof KisApiException) {
                        return throwable;
                    }
                    return new KisApiException("웹소켓 접속키 발급 실패", throwable);
                });
    }
    
    /**
     * approval_key의 일부를 마스킹하여 로그에 안전하게 출력합니다.
     * 
     * @param approvalKey 원본 approval_key
     * @return 마스킹된 approval_key
     */
    private String maskApprovalKey(String approvalKey) {
        if (approvalKey == null || approvalKey.length() < 8) {
            return "****";
        }
        return approvalKey.substring(0, 4) + "****" + approvalKey.substring(approvalKey.length() - 4);
    }
    
    /**
     * API 키의 일부를 마스킹하여 로그에 안전하게 출력합니다.
     * 
     * @param key 원본 키
     * @return 마스킹된 키
     */
    private String maskKey(String key) {
        if (key == null || key.length() < 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
    
    /**
     * HTTP 상태 코드와 응답 본문을 기반으로 에러 메시지를 생성합니다.
     */
    private String createErrorMessage(int statusCode, String responseBody) {
        String baseMessage = switch (statusCode) {
            case 400 -> "잘못된 요청";
            case 401 -> "인증 실패 - API 키 또는 시크릿이 올바르지 않습니다";
            case 403 -> "접근 권한 없음 - API 키 권한을 확인하거나 모의투자/실투자 환경 설정을 확인하세요";
            case 429 -> "요청 한도 초과 - 잠시 후 다시 시도하세요";
            case 500 -> "서버 내부 오류";
            case 502 -> "Bad Gateway - 서버 연결 오류";
            case 503 -> "서비스 일시 중단";
            default -> "API 호출 실패";
        };
        
        return String.format("%s (HTTP %d): %s", baseMessage, statusCode, responseBody);
    }
    
    /**
     * Mock 서비스 사용 여부 결정
     */
    private boolean shouldUseMockService() {
        // 테스트 키를 사용하거나 키가 유효하지 않은 경우 Mock 서비스 사용
        return kisMockService != null && 
               (!kisMockService.isValidApiKey(kisApiProperties.appKey(), kisApiProperties.appSecret()) ||
                "test-app-key".equals(kisApiProperties.appKey()));
    }
}