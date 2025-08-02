package com.stock.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisTokenRequest(
    @JsonProperty("grant_type")
    String grantType,
    
    @JsonProperty("appkey")
    String appKey,
    
    @JsonProperty("appsecret") 
    String appSecret
) {
    
    public static KisTokenRequest of(String appKey, String appSecret) {
        return new KisTokenRequest("client_credentials", appKey, appSecret);
    }
}