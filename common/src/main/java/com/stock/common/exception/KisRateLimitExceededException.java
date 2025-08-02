package com.stock.common.exception;

public class KisRateLimitExceededException extends KisApiException {
    
    private final int retryAfterSeconds;
    
    public KisRateLimitExceededException(int retryAfterSeconds) {
        super("KIS API rate limit exceeded. Retry after " + retryAfterSeconds + " seconds");
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public KisRateLimitExceededException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}