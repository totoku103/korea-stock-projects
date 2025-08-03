
package com.stock.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KisRealTimeSubscriptionRequest(
    Header header,
    Body body
) {

    public record Header(
        @JsonProperty("approval_key") String approvalKey,
        @JsonProperty("custtype") String customerType,
        @JsonProperty("tr_type") String transactionType,
        @JsonProperty("content-type") String contentType
    ) {
        public Header(String approvalKey, String transactionType) {
            this(approvalKey, "P", transactionType, "utf-8");
        }
    }

    public record Body(
        @JsonProperty("input") Input input
    ) {}

    public record Input(
        @JsonProperty("tr_id") String transactionId,
        @JsonProperty("tr_key") String transactionKey
    ) {}

    public static KisRealTimeSubscriptionRequest execution(String approvalKey, String stockCode) {
        Header header = new Header(approvalKey, "1"); // 1: 실시간 체결가
        Input input = new Input("H0STCNT0", stockCode);
        Body body = new Body(input);
        return new KisRealTimeSubscriptionRequest(header, body);
    }
}
