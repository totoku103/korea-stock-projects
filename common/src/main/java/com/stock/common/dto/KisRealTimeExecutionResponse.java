
package com.stock.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisRealTimeExecutionResponse(
    @JsonProperty("header") Header header,
    @JsonProperty("body") Body body
) {

    public record Header(
        @JsonProperty("tr_id") String transactionId,
        @JsonProperty("tr_key") String transactionKey,
        @JsonProperty("encrypt") String encrypt,
        @JsonProperty("datetime") String datetime
    ) {}

    public record Body(
        @JsonProperty("output") Output output,
        @JsonProperty("rt_cd") String returnCode,
        @JsonProperty("msg_cd") String messageCode,
        @JsonProperty("msg1") String message
    ) {}

    public record Output(
        // 체결 시간 (HHMMSS)
        @JsonProperty("STCK_CNTG_HOUR") String executionTime,
        // 주식 현재가
        @JsonProperty("STCK_PRPR") String price,
        // 전일 대비
        @JsonProperty("PRDY_VRSS") String changeFromYesterday,
        // 전일 대비 부호 (1: 상한, 2: 상승, 3: 보합, 4: 하한, 5: 하락)
        @JsonProperty("PRDY_VRSS_SIGN") String changeSign,
        // 전일 대비율
        @JsonProperty("PRDY_CTRT") String changeRate,
        // 누적 거래량
        @JsonProperty("ACML_VOL") String accumulatedVolume,
        // 누적 거래 대금
        @JsonProperty("ACML_TR_PBMN") String accumulatedTradeValue,
        // 체결 거래량
        @JsonProperty("CNTG_VOL") String executionVolume
    ) {}

    public boolean isSuccessful() {
        return "0".equals(returnCode);
    }
}
