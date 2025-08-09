package com.stock.batch.service;

import com.stock.common.constants.KisApiConstants;
import com.stock.common.dto.KisApiResponse;
import com.stock.common.service.KisTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class KisApiTestService {
    
    private static final Logger logger = LoggerFactory.getLogger(KisApiTestService.class);
    
    private final WebClient webClient;
    private final KisTokenService tokenService;
    
    public KisApiTestService(WebClient kisApiWebClient, KisTokenService tokenService) {
        this.webClient = kisApiWebClient;
        this.tokenService = tokenService;
    }
    
    /**
     * API 연결 테스트 - 토큰 발급 및 기본 주식 정보 조회
     */
    public Mono<Map<String, Object>> testApiConnection() {
        logger.info("Starting API connection test");
        
        return tokenService.requestNewToken()
            .flatMap(tokenResponse -> {
                logger.info("Token obtained successfully: {}", tokenResponse.getBearerToken());
                
                // 삼성전자(005930) 주식 기본 정보 조회 테스트
                return testStockPriceInquiry("005930");
            })
            .map(response -> Map.of(
                "success", true,
                "message", "API connection test successful",
                "tokenValid", tokenService.hasValidToken(),
                "tokenExpiration", tokenService.getTokenExpirationTime(),
                "stockData", response
            ))
            .onErrorResume(throwable -> {
                Map<String, Object> errorResult = Map.of(
                    "success", false,
                    "message", "API connection test failed: " + throwable.getMessage(),
                    "tokenValid", false
                );
                return Mono.just(errorResult);
            });
    }
    
    /**
     * 주식 현재가 조회 테스트
     */
    public Mono<Object> testStockPriceInquiry(String stockCode) {
        return tokenService.getValidAccessToken()
            .flatMap(token -> webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(KisApiConstants.STOCK_PRICE_ENDPOINT)
                    .queryParam("FID_COND_MRKT_DIV_CODE", KisApiConstants.MARKET_CODE_KOSPI)
                    .queryParam("FID_INPUT_ISCD", stockCode)
                    .build())
                .header(KisApiConstants.HEADER_AUTHORIZATION, token)
                .header(KisApiConstants.HEADER_TR_ID, KisApiConstants.TR_ID_STOCK_PRICE)
                .retrieve()
                .bodyToMono(KisApiResponse.class)
                .doOnSuccess(response -> {
                    if (response.isSuccess()) {
                        logger.info("Stock price inquiry successful for code: {}", stockCode);
                    } else {
                        logger.warn("Stock price inquiry failed: {} - {}", 
                            response.messageCode(), response.message());
                    }
                })
                .map(KisApiResponse::getMainOutput));
    }
    
    /**
     * 토큰 상태 확인
     */
    public Map<String, Object> getTokenStatus() {
        return Map.of(
            "hasValidToken", tokenService.hasValidToken(),
            "tokenExpiration", tokenService.getTokenExpirationTime()
        );
    }
    
    /**
     * 수동 토큰 갱신
     */
    public Mono<Map<String, Object>> refreshToken() {
        return tokenService.requestNewToken()
            .map(tokenResponse -> {
                Map<String, Object> result = Map.of(
                    "success", true,
                    "message", "Token refreshed successfully",
                    "expiresAt", tokenResponse.getExpiresAt()
                );
                return result;
            })
            .onErrorResume(throwable -> {
                Map<String, Object> errorResult = Map.of(
                    "success", false,
                    "message", "Token refresh failed: " + throwable.getMessage()
                );
                return Mono.just(errorResult);
            });
    }
}