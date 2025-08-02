package com.stock.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisWebSocketKeyRequest(
    @JsonProperty("grant_type")
    String grantType,
    
    @JsonProperty("appkey")
    String appKey,
    
    @JsonProperty("secretkey")
    String secretKey
) {
    
    public static KisWebSocketKeyRequest of(String appKey, String secretKey) {
        return new KisWebSocketKeyRequest("client_credentials", appKey, secretKey);
    }
}