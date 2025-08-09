package com.stock.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 주식현재가 시세 조회 응답 DTO
 * 한국투자증권 API: v1_국내주식-008
 */
public record KisStockPriceResponse(
    @JsonProperty("rt_cd") String returnCode,
    @JsonProperty("msg_cd") String messageCode,
    @JsonProperty("msg1") String message,
    @JsonProperty("output") Output output
) {
    
    public record Output(
        // 현재가 정보
        @JsonProperty("stck_prpr") String currentPrice,           // 주식 현재가
        @JsonProperty("prdy_vrss") String priceChange,            // 전일 대비
        @JsonProperty("prdy_vrss_sign") String priceChangeSign,   // 전일 대비 부호
        @JsonProperty("prdy_ctrt") String priceChangeRate,        // 전일 대비율
        
        // 호가 정보
        @JsonProperty("askp1") String askPrice1,                  // 매도호가1
        @JsonProperty("bidp1") String bidPrice1,                  // 매수호가1
        @JsonProperty("askp_rsqn1") String askQuantity1,          // 매도호가 잔량1
        @JsonProperty("bidp_rsqn1") String bidQuantity1,          // 매수호가 잔량1
        
        // 거래량 정보
        @JsonProperty("acml_vol") String accumulatedVolume,       // 누적 거래량
        @JsonProperty("acml_tr_pbmn") String accumulatedValue,    // 누적 거래대금
        @JsonProperty("seln_cntg_csnu") String sellVolume,        // 매도 체결 수량
        @JsonProperty("shnu_cntg_csnu") String buyVolume,         // 매수 체결 수량
        
        // 가격 범위
        @JsonProperty("stck_oprc") String openPrice,              // 시가
        @JsonProperty("stck_hgpr") String highPrice,              // 고가
        @JsonProperty("stck_lwpr") String lowPrice,               // 저가
        @JsonProperty("stck_mxpr") String maxPrice,               // 상한가
        @JsonProperty("stck_llam") String minPrice,               // 하한가
        
        // 기본 정보
        @JsonProperty("stck_shrn_iscd") String stockCode,         // 주식 단축 종목코드
        @JsonProperty("hts_kor_isnm") String stockName,           // 종목명
        @JsonProperty("rprs_mrkt_kor_name") String marketName,    // 대표 시장 한글명
        
        // 시가총액
        @JsonProperty("hts_avls") String marketCap,               // 시가총액
        @JsonProperty("lstn_stcn") String listedShares,           // 상장주수
        
        // 거래 상태
        @JsonProperty("iscd_stat_cls_code") String statusCode,    // 종목 상태 구분 코드
        @JsonProperty("marg_rate") String marginRate,             // 증거금 비율
        
        // 시간 정보
        @JsonProperty("stck_cntg_hour") String transactionTime,   // 주식 체결 시간
        @JsonProperty("vi_cls_code") String viCode                // VI적용구분코드
    ) {}
    
    /**
     * 응답이 성공인지 확인
     */
    public boolean isSuccessful() {
        return "0".equals(returnCode);
    }
    
    /**
     * 에러 메시지 반환
     */
    public String getErrorMessage() {
        if (isSuccessful()) {
            return null;
        }
        return String.format("[%s] %s", messageCode, message);
    }
}