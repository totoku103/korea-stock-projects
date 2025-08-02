package com.stock.common.exception;

public class KisApiException extends RuntimeException {
    
    private final String errorCode;
    private final String messageCode;
    
    public KisApiException(String message) {
        super(message);
        this.errorCode = null;
        this.messageCode = null;
    }
    
    public KisApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.messageCode = null;
    }
    
    public KisApiException(String errorCode, String messageCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.messageCode = messageCode;
    }
    
    public KisApiException(String errorCode, String messageCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.messageCode = messageCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getMessageCode() {
        return messageCode;
    }
}