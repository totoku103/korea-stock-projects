package com.stock.common.exception;

public class KisWebSocketException extends KisApiException {
    
    public KisWebSocketException(String message) {
        super(message);
    }
    
    public KisWebSocketException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public KisWebSocketException(String errorCode, String messageCode, String message) {
        super(errorCode, messageCode, message);
    }
    
    public KisWebSocketException(String errorCode, String messageCode, String message, Throwable cause) {
        super(errorCode, messageCode, message, cause);
    }
}