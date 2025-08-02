package com.stock.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisApiResponse<T>(
    @JsonProperty("rt_cd")
    String returnCode,
    
    @JsonProperty("msg_cd")
    String messageCode,
    
    @JsonProperty("msg1")
    String message,
    
    @JsonProperty("output")
    T output,
    
    @JsonProperty("output1")
    T output1,
    
    @JsonProperty("output2")
    T output2
) {
    
    public boolean isSuccess() {
        return "0".equals(returnCode);
    }
    
    public boolean isError() {
        return !isSuccess();
    }
    
    public T getMainOutput() {
        if (output != null) return output;
        if (output1 != null) return output1;
        return output2;
    }
}