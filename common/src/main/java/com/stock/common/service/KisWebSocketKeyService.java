package com.stock.common.service;

import com.stock.common.config.KisApiProperties;
import com.stock.common.constants.KisApiConstants;
import com.stock.common.dto.KisWebSocketKeyRequest;
import com.stock.common.dto.KisWebSocketKeyResponse;
import com.stock.common.exception.KisApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        
        KisWebSocketKeyRequest request = KisWebSocketKeyRequest.of(
            kisApiProperties.appKey(),
            kisApiProperties.appSecret()
        );
        
        return webClient.post()
                .uri(kisApiProperties.baseUrl() + KisApiConstants.WEBSOCKET_APPROVAL_KEY_ENDPOINT)
                .header(KisApiConstants.HEADER_CONTENT_TYPE, "application/json; charset=utf-8")
                .bodyValue(request)
                .retrieve()
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
}