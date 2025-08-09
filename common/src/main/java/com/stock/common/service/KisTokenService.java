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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class KisTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(KisTokenService.class);

    private final WebClient webClient;
    private final KisApiProperties kisApiProperties;
    private final AtomicReference<KisTokenResponse> currentToken = new AtomicReference<>();

    public KisTokenService(WebClient kisApiWebClient, KisApiProperties kisApiProperties) {
        this.webClient = kisApiWebClient;
        this.kisApiProperties = kisApiProperties;
    }

    /**
     * 유효한 액세스 토큰 반환
     * 토큰이 없거나 만료된 경우 자동으로 갱신
     */
    public Mono<String> getValidAccessToken() {
        KisTokenResponse token = currentToken.get();

        // 토큰이 없거나 만료된 경우 새로 발급
        if (token == null || token.isExpired()) {
            logger.info("Token is null or expired, requesting new token");
            return requestNewToken()
                .map(KisTokenResponse::getBearerToken);
        }

        // 토큰이 30분 이내에 만료될 예정이면 미리 갱신
        if (token.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(30))) {
            logger.info("Token will expire soon, refreshing token proactively");
            return requestNewToken()
                .map(KisTokenResponse::getBearerToken)
                .onErrorReturn(token.getBearerToken()); // 갱신 실패 시 기존 토큰 사용
        }

        return Mono.just(token.getBearerToken());
    }

    // --- Backward-compatible APIs for existing controllers ---
    /**
     * Legacy: issue a new access token (compat shim for older callers)
     */
    public Mono<KisTokenResponse> issueNewAccessToken() {
        return requestNewToken();
    }

    /**
     * Legacy: get access token (returns full response for compatibility)
     */
    public Mono<KisTokenResponse> getAccessToken() {
        return requestNewToken();
    }

    /**
     * 새로운 액세스 토큰 요청
     */
    public Mono<KisTokenResponse> requestNewToken() {
        KisTokenRequest request = KisTokenRequest.of(
            kisApiProperties.appKey(),
            kisApiProperties.appSecret()
        );

        return webClient.post()
            .uri(KisApiConstants.TOKEN_ENDPOINT)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(KisTokenResponse.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .filter(this::isRetryableException))
            .doOnSuccess(tokenResponse -> {
                if (tokenResponse != null) {
                    currentToken.set(tokenResponse);
                    logger.info("Successfully obtained new access token, expires at: {}",
                        tokenResponse.getExpiresAt());
                }
            })
            .doOnError(error -> {
                logger.error("Failed to obtain access token", error);
                currentToken.set(null);
            })
            .onErrorMap(WebClientResponseException.class, this::mapWebClientException)
            .onErrorMap(Exception.class, ex -> new KisApiException("Failed to obtain access token", ex));
    }

    /**
     * 토큰 폐기
     */
    public Mono<Void> revokeToken() {
        KisTokenResponse token = currentToken.get();
        if (token == null) {
            return Mono.empty();
        }

        return webClient.post()
            .uri(KisApiConstants.TOKEN_REVOKE_ENDPOINT)
            .header(KisApiConstants.HEADER_AUTHORIZATION, token.getBearerToken())
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(unused -> {
                currentToken.set(null);
                logger.info("Successfully revoked access token");
            })
            .doOnError(error -> logger.warn("Failed to revoke access token", error))
            .onErrorResume(ex -> Mono.empty()); // 폐기 실패는 무시
    }

    /**
     * Legacy: revoke a provided token string. We ignore the argument and
     * revoke the currently cached token to maintain behavior.
     */
    public Mono<Void> revokeToken(String token) {
        return revokeToken();
    }

    /**
     * 현재 토큰 상태 확인
     */
    public boolean hasValidToken() {
        KisTokenResponse token = currentToken.get();
        return token != null && !token.isExpired();
    }

    /**
     * 토큰 만료 시간 반환
     */
    public LocalDateTime getTokenExpirationTime() {
        KisTokenResponse token = currentToken.get();
        return token != null ? token.getExpiresAt() : null;
    }

    /**
     * Legacy: invalidate cached token explicitly
     */
    public void invalidateCache() {
        currentToken.set(null);
    }

    /**
     * Legacy: return cached token response if present
     */
    public KisTokenResponse getCachedToken() {
        return currentToken.get();
    }

    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            // 5xx 서버 에러나 429 Rate Limit의 경우 재시도
            return wcre.getStatusCode().is5xxServerError() || wcre.getStatusCode().value() == 429;
        }
        return false;
    }

    private KisApiException mapWebClientException(WebClientResponseException ex) {
        if (ex.getStatusCode().value() == 401) {
            return new KisTokenExpiredException("Authentication failed: " + ex.getResponseBodyAsString());
        } else if (ex.getStatusCode().value() == 429) {
            return new KisApiException("Rate limit exceeded: " + ex.getResponseBodyAsString());
        } else {
            return new KisApiException("API request failed: " + ex.getResponseBodyAsString(), ex);
        }
    }
}