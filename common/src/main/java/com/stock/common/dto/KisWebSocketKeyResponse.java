package com.stock.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record KisWebSocketKeyResponse(
    @JsonProperty("approval_key")
    String approvalKey,
    
    @JsonProperty("msg_cd")
    String messageCode,
    
    @JsonProperty("msg1")
    String message,
    
    LocalDateTime issuedAt
) {
    
    public KisWebSocketKeyResponse(String approvalKey, String messageCode, String message) {
        this(approvalKey, messageCode, message, LocalDateTime.now());
    }
    
    public boolean isSuccessful() {
        return "0".equals(messageCode);
    }
    
    public boolean hasApprovalKey() {
        return approvalKey != null && !approvalKey.trim().isEmpty();
    }
}