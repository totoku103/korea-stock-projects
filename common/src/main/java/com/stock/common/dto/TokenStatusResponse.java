package com.stock.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record TokenStatusResponse(
    @JsonProperty("has_valid_token")
    boolean hasValidToken,
    
    @JsonProperty("token_type")
    String tokenType,
    
    @JsonProperty("expires_at")
    LocalDateTime expiresAt,
    
    @JsonProperty("issued_at")
    LocalDateTime issuedAt,
    
    @JsonProperty("expires_in_seconds")
    long expiresInSeconds
) {
    
    public static TokenStatusResponse fromTokenResponse(KisTokenResponse tokenResponse) {
        if (tokenResponse == null) {
            return new TokenStatusResponse(false, null, null, null, 0L);
        }
        
        return new TokenStatusResponse(
            !tokenResponse.isExpired(),
            tokenResponse.tokenType(),
            tokenResponse.getExpiresAt(),
            tokenResponse.issuedAt(),
            tokenResponse.expiresIn()
        );
    }
    
    public static TokenStatusResponse noToken() {
        return new TokenStatusResponse(false, null, null, null, 0L);
    }
}