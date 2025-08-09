package com.stock.common.util;

import com.stock.common.config.KisApiProperties;
import com.stock.common.constants.KisApiConstants;

public final class KisWebSocketUtil {

    private KisWebSocketUtil() {
    }

    /**
     * 웹소켓 연결을 위한 헤더 정보를 생성합니다.
     *
     * @param approvalKey 웹소켓 접속키
     * @param custtype    고객구분 (P: 개인, B: 법인)
     * @return 헤더 맵
     */
    public static java.util.Map<String, String> createWebSocketHeaders(String approvalKey, String custtype) {
        return java.util.Map.of(
                "approval_key", approvalKey,
                "custtype", custtype
        );
    }

    /**
     * 실시간 시세 구독을 위한 메시지를 생성합니다.
     *
     * @param stockCode 종목코드
     * @param trType    거래구분 (1: 등록, 2: 해제)
     * @return 구독 메시지
     */
    public static String createStockPriceSubscriptionMessage(String stockCode, String trType) {
        return String.format("{\"header\":{\"approval_key\":\"%s\",\"custtype\":\"P\",\"tr_type\":\"%s\",\"content-type\":\"utf-8\"},\"body\":{\"input\":{\"tr_id\":\"H0STCNT0\",\"tr_key\":\"%s\"}}}",
                "", trType, stockCode);
    }

    /**
     * 실시간 호가 구독을 위한 메시지를 생성합니다.
     *
     * @param stockCode 종목코드
     * @param trType    거래구분 (1: 등록, 2: 해제)
     * @return 구독 메시지
     */
    public static String createOrderbookSubscriptionMessage(String stockCode, String trType) {
        return String.format("{\"header\":{\"approval_key\":\"%s\",\"custtype\":\"P\",\"tr_type\":\"%s\",\"content-type\":\"utf-8\"},\"body\":{\"input\":{\"tr_id\":\"H0STASP0\",\"tr_key\":\"%s\"}}}",
                "", trType, stockCode);
    }
}