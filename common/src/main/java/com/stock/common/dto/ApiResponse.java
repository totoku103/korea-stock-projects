package com.stock.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record ApiResponse<T>(
    @JsonProperty("success")
    boolean success,
    
    @JsonProperty("message")
    String message,
    
    @JsonProperty("data")
    T data,
    
    @JsonProperty("error_code")
    String errorCode,
    
    @JsonProperty("timestamp")
    LocalDateTime timestamp
) {
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "성공", data, null, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, errorCode, LocalDateTime.now());
    }
}