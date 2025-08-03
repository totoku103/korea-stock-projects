package com.stock.common.service;

import com.stock.common.config.KisApiProperties;
import com.stock.common.constants.KisApiConstants;
import com.stock.common.dto.KisTokenRequest;
import com.stock.common.dto.KisTokenResponse;
import com.stock.common.exception.KisApiException;
import com.stock.common.exception.KisTokenExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class KisTokenService {
    
    private static final Logger log = LoggerFactory.getLogger(KisTokenService.class);
    
    private final WebClient webClient;
    private final KisApiProperties kisApiProperties;
    private final AtomicReference<KisTokenResponse> cachedToken = new AtomicReference<>();
    
    public KisTokenService(WebClient webClient, KisApiProperties kisApiProperties) {
        this.webClient = webClient;
        this.kisApiProperties = kisApiProperties;
    }
    
    /**
     * OAuth2 액세스 토큰을 발급받습니다.
     * 캐시된 토큰이 유효한 경우 재사용하고, 만료된 경우 새로 발급받습니다.
     * 
     * @return OAuth2 토큰 응답
     */
    public Mono<KisTokenResponse> getAccessToken() {
        KisTokenResponse cached = cachedToken.get();
        
        if (cached != null && !cached.isExpired()) {
            log.debug("유효한 캐시 토큰 사용: expiresAt={}", cached.getExpiresAt());
            return Mono.just(cached);
        }
        
        log.info("새 액세스 토큰 발급 요청");
        return issueNewAccessToken()
                .doOnNext(token -> {
                    cachedToken.set(token);
                    log.info("새 액세스 토큰 발급 완료: expiresAt={}", token.getExpiresAt());
                });
    }
    
    /**
     * 새로운 액세스 토큰을 발급받습니다.
     * 
     * @return 새 OAuth2 토큰 응답
     */
    public Mono<KisTokenResponse> issueNewAccessToken() {
        log.info("액세스 토큰 발급 요청 시작");
        
        KisTokenRequest request = KisTokenRequest.of(
            kisApiProperties.appKey(),
            kisApiProperties.appSecret()
        );
        
        return webClient.post()
                .uri(kisApiProperties.baseUrl() + KisApiConstants.TOKEN_ENDPOINT)
                .header(KisApiConstants.HEADER_CONTENT_TYPE, "application/json; charset=utf-8")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(KisTokenResponse.class)
                .doOnNext(response -> {
                    log.info("액세스 토큰 발급 성공: tokenType={}, expiresIn={}초", 
                           response.tokenType(), 
                           response.expiresIn());
                })
                .doOnError(error -> log.error("액세스 토큰 발급 중 오류 발생", error))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .maxBackoff(Duration.ofSeconds(10))
                    .filter(throwable -> !(throwable instanceof KisApiException)))
                .onErrorMap(throwable -> {
                    if (throwable instanceof KisApiException) {
                        return throwable;
                    }
                    return new KisApiException("액세스 토큰 발급 실패", throwable);
                });
    }
    
    /**
     * 토큰을 무효화합니다.
     * 
     * @param token 무효화할 토큰
     * @return 무효화 완료 신호
     */
    public Mono<Void> revokeToken(String token) {
        log.info("토큰 무효화 요청");
        
        return webClient.post()
                .uri(kisApiProperties.baseUrl() + KisApiConstants.TOKEN_REVOKE_ENDPOINT)
                .header(KisApiConstants.HEADER_CONTENT_TYPE, "application/json; charset=utf-8")
                .header(KisApiConstants.HEADER_AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(result -> {
                    log.info("토큰 무효화 완료");
                    cachedToken.set(null);
                })
                .doOnError(error -> log.error("토큰 무효화 중 오류 발생", error))
                .onErrorMap(throwable -> new KisApiException("토큰 무효화 실패", throwable));
    }
    
    /**
     * 캐시된 토큰을 강제로 무효화합니다.
     */
    public void invalidateCache() {
        log.info("토큰 캐시 무효화");
        cachedToken.set(null);
    }
    
    /**
     * 유효한 토큰이 캐시되어 있는지 확인합니다.
     * 
     * @return 유효한 토큰 존재 여부
     */
    public boolean hasValidToken() {
        KisTokenResponse cached = cachedToken.get();
        return cached != null && !cached.isExpired();
    }
    
    /**
     * 현재 캐시된 토큰을 반환합니다.
     * 
     * @return 캐시된 토큰 (없으면 null)
     */
    public KisTokenResponse getCachedToken() {
        return cachedToken.get();
    }
}