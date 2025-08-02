package com.stock.common.exception;

public class KisTokenExpiredException extends KisApiException {
    
    public KisTokenExpiredException() {
        super("KIS API token has expired");
    }
    
    public KisTokenExpiredException(String message) {
        super(message);
    }
    
    public KisTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}