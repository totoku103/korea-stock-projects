package com.stock.common.exception;

/**
 * 한국투자증권 API 인증 관련 예외
 * 401 Unauthorized, 403 Forbidden 등의 인증/인가 오류 시 사용
 */
public class KisApiAuthenticationException extends KisApiException {
    
    public KisApiAuthenticationException(String message) {
        super(message);
    }
    
    public KisApiAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}