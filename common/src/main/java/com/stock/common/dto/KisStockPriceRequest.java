package com.stock.common.dto;

/**
 * 주식현재가 시세 조회 요청 DTO
 * 한국투자증권 API: v1_국내주식-008
 */
public record KisStockPriceRequest(
    String stockCode,  // 종목코드 (6자리)
    String market      // 시장구분 (J: 코스피, Q: 코스닥)
) {
    
    public KisStockPriceRequest {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            throw new IllegalArgumentException("종목코드는 필수입니다");
        }
        if (market == null || (!market.equals("J") && !market.equals("Q"))) {
            throw new IllegalArgumentException("시장구분은 J(코스피) 또는 Q(코스닥)만 가능합니다");
        }
    }
    
    /**
     * 코스피 종목 요청 생성
     */
    public static KisStockPriceRequest kospi(String stockCode) {
        return new KisStockPriceRequest(stockCode, "J");
    }
    
    /**
     * 코스닥 종목 요청 생성
     */
    public static KisStockPriceRequest kosdaq(String stockCode) {
        return new KisStockPriceRequest(stockCode, "Q");
    }
}