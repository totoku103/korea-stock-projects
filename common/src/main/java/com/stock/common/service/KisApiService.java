package com.stock.common.service;

import com.stock.common.dto.KisStockPriceRequest;
import com.stock.common.dto.KisStockPriceResponse;
import com.stock.common.exception.KisApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * 한국투자증권 API 서비스
 * 주식 시세, 차트, 호가 등의 시장 데이터 조회 기능 제공
 */
@Service
public class KisApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(KisApiService.class);
    
    private final WebClient webClient;
    private final KisTokenService tokenService;
    
    // API 엔드포인트 상수
    private static final String STOCK_PRICE_ENDPOINT = "/uapi/domestic-stock/v1/quotations/inquire-price";
    
    // 헤더 상수
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_APP_KEY = "appkey";
    private static final String HEADER_APP_SECRET = "appsecret";
    private static final String HEADER_TR_ID = "tr_id";
    private static final String HEADER_CUSTTYPE = "custtype";
    
    // TR_ID 상수 (한국투자증권 거래ID)
    private static final String TR_ID_STOCK_PRICE = "FHKST01010100";  // 주식현재가 시세

    public KisApiService(WebClient kisApiWebClient, KisTokenService tokenService) {
        this.webClient = kisApiWebClient;
        this.tokenService = tokenService;
    }

    /**
     * 주식현재가 시세 조회
     * @param request 종목코드 및 시장구분 정보
     * @return 주식 현재가 정보
     */
    public Mono<KisStockPriceResponse> getStockPrice(KisStockPriceRequest request) {
        logger.debug("주식현재가 시세 조회 요청: 종목코드={}, 시장={}", request.stockCode(), request.market());
        
        return tokenService.getValidAccessToken()
            .flatMap(accessToken -> {
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path(STOCK_PRICE_ENDPOINT)
                        .queryParam("FID_COND_MRKT_DIV_CODE", request.market())
                        .queryParam("FID_INPUT_ISCD", request.stockCode())
                        .build())
                    .header(HEADER_AUTHORIZATION, "Bearer " + accessToken)
                    .header(HEADER_TR_ID, TR_ID_STOCK_PRICE)
                    .header(HEADER_CUSTTYPE, "P")  // P: 개인
                    .retrieve()
                    .bodyToMono(KisStockPriceResponse.class);
            })
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .filter(this::isRetryableException))
            .doOnSuccess(response -> {
                if (response != null && response.isSuccessful()) {
                    logger.debug("주식현재가 시세 조회 성공: 종목코드={}, 현재가={}", 
                        request.stockCode(), response.output().currentPrice());
                } else {
                    logger.warn("주식현재가 시세 조회 실패: 종목코드={}, 오류={}", 
                        request.stockCode(), response != null ? response.getErrorMessage() : "알 수 없는 오류");
                }
            })
            .doOnError(error -> {
                logger.error("주식현재가 시세 조회 중 오류 발생: 종목코드={}", request.stockCode(), error);
            })
            .onErrorMap(WebClientResponseException.class, this::mapWebClientException)
            .onErrorMap(Exception.class, ex -> 
                new KisApiException("주식현재가 시세 조회 실패: " + ex.getMessage(), ex));
    }
    
    /**
     * 코스피 종목의 현재가 조회
     */
    public Mono<KisStockPriceResponse> getKospiStockPrice(String stockCode) {
        return getStockPrice(KisStockPriceRequest.kospi(stockCode));
    }
    
    /**
     * 코스닥 종목의 현재가 조회
     */
    public Mono<KisStockPriceResponse> getKosdaqStockPrice(String stockCode) {
        return getStockPrice(KisStockPriceRequest.kosdaq(stockCode));
    }

    /**
     * 재시도 가능한 예외 판단
     */
    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            // 5xx 서버 에러나 429 Rate Limit의 경우 재시도
            return wcre.getStatusCode().is5xxServerError() || 
                   wcre.getStatusCode().value() == 429;
        }
        return false;
    }

    /**
     * WebClient 예외를 KisApi 예외로 변환
     */
    private KisApiException mapWebClientException(WebClientResponseException ex) {
        String errorMessage = String.format("API 요청 실패 [%d]: %s", 
            ex.getStatusCode().value(), ex.getResponseBodyAsString());
        
        return switch (ex.getStatusCode().value()) {
            case 400 -> new KisApiException("잘못된 요청: " + errorMessage);
            case 401 -> new KisApiException("인증 실패: " + errorMessage);
            case 403 -> new KisApiException("접근 권한 없음: " + errorMessage);
            case 429 -> new KisApiException("요청 한도 초과: " + errorMessage);
            default -> new KisApiException("API 요청 오류: " + errorMessage, ex);
        };
    }
}