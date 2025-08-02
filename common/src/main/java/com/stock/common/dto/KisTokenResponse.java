package com.stock.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record KisTokenResponse(
    @JsonProperty("access_token")
    String accessToken,
    
    @JsonProperty("token_type")
    String tokenType,
    
    @JsonProperty("expires_in")
    long expiresIn,
    
    @JsonProperty("access_token_token_expired")
    String accessTokenExpired,
    
    LocalDateTime issuedAt
) {
    
    public KisTokenResponse(String accessToken, String tokenType, long expiresIn, String accessTokenExpired) {
        this(accessToken, tokenType, expiresIn, accessTokenExpired, LocalDateTime.now());
    }
    
    public boolean isExpired() {
        return issuedAt.plusSeconds(expiresIn).isBefore(LocalDateTime.now());
    }
    
    public LocalDateTime getExpiresAt() {
        return issuedAt.plusSeconds(expiresIn);
    }
    
    public String getBearerToken() {
        return "Bearer " + accessToken;
    }
}