package com.stock.batch.scheduler;

import com.stock.common.config.KisApiProperties;
import com.stock.common.dto.KisStockPriceRequest;
import com.stock.common.dto.KisStockPriceResponse;
import com.stock.common.service.KisApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 매 1분마다 배치 Job 실행 스케줄러
 */
@Configuration
@EnableScheduling
public class StockPriceMinuteScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockPriceMinuteScheduler.class);
    
    // 삼성전자 종목코드
    private static final String SAMSUNG_STOCK_CODE = "005930";

    private final JobLauncher jobLauncher;
    private final Job stockPriceMinuteJob;
    private final KisApiService kisApiService;
    private final KisApiProperties kisApiProperties;

    public StockPriceMinuteScheduler(JobLauncher jobLauncher, Job stockPriceMinuteJob, 
                                   KisApiService kisApiService, KisApiProperties kisApiProperties) {
        this.jobLauncher = jobLauncher;
        this.stockPriceMinuteJob = stockPriceMinuteJob;
        this.kisApiService = kisApiService;
        this.kisApiProperties = kisApiProperties;
    }

    // 초(0)마다 1분 간격 실행
    @Scheduled(cron = "0 * * * * *")
    public void runStockPriceMinuteJob() {
        try {
            // 삼성전자 현재가 조회
            getSamsungStockPrice();
            
            // 기존 배치 Job 실행
            JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis()) // RunId 대체 키
                .toJobParameters();

            log.info("[스케줄러] 1분 주기 배치 시작");
            jobLauncher.run(stockPriceMinuteJob, params);
        } catch (Exception e) {
            log.error("[스케줄러] 1분 주기 배치 실행 실패", e);
        }
    }
    
    /**
     * 삼성전자 현재가 조회 및 콘솔 출력
     */
    private void getSamsungStockPrice() {
        try {
            // API 설정 정보 출력
            logApiConfiguration();
            
            log.info("[삼성전자 시세 조회] 시작 - 종목코드: {}", SAMSUNG_STOCK_CODE);
            
            // 삼성전자는 코스피 종목이므로 kospi 메서드 사용
            KisStockPriceResponse response = kisApiService.getKospiStockPrice(SAMSUNG_STOCK_CODE)
                .block(); // 동기적으로 처리
            
            if (response != null && response.isSuccessful()) {
                printStockPriceInfo(response);
            } else {
                String errorMessage = response != null ? response.getErrorMessage() : "응답 없음";
                log.error("[삼성전자 시세 조회] 실패: {}", errorMessage);
            }
            
        } catch (Exception e) {
            log.error("[삼성전자 시세 조회] 예외 발생", e);
        }
    }
    
    /**
     * 주식 시세 정보 콘솔 출력
     */
    private void printStockPriceInfo(KisStockPriceResponse response) {
        KisStockPriceResponse.Output output = response.output();
        
        log.info("============= 삼성전자 현재가 정보 =============");
        log.info("종목명: {}", output.stockName());
        log.info("종목코드: {}", output.stockCode());
        log.info("현재가: {}원", formatPrice(output.currentPrice()));
        log.info("전일대비: {} ({}%)", formatPrice(output.priceChange()), output.priceChangeRate());
        log.info("등락률 부호: {}", getPriceChangeSignText(output.priceChangeSign()));
        log.info("시가: {}원", formatPrice(output.openPrice()));
        log.info("고가: {}원", formatPrice(output.highPrice()));
        log.info("저가: {}원", formatPrice(output.lowPrice()));
        log.info("거래량: {}", formatVolume(output.accumulatedVolume()));
        log.info("거래대금: {}원", formatPrice(output.accumulatedValue()));
        log.info("체결시간: {}", output.transactionTime());
        log.info("===========================================");
    }
    
    /**
     * 가격 포맷팅 (천 단위 구분)
     */
    private String formatPrice(String price) {
        if (price == null || price.trim().isEmpty()) {
            return "0";
        }
        try {
            long longPrice = Long.parseLong(price);
            return String.format("%,d", longPrice);
        } catch (NumberFormatException e) {
            return price;
        }
    }
    
    /**
     * 거래량 포맷팅
     */
    private String formatVolume(String volume) {
        if (volume == null || volume.trim().isEmpty()) {
            return "0";
        }
        try {
            long longVolume = Long.parseLong(volume);
            return String.format("%,d", longVolume);
        } catch (NumberFormatException e) {
            return volume;
        }
    }
    
    /**
     * 등락률 부호 텍스트 변환
     */
    private String getPriceChangeSignText(String sign) {
        if (sign == null) return "보합";
        
        return switch (sign) {
            case "1" -> "▲ 상승";
            case "2" -> "▼ 하락";
            case "3" -> "→ 보합";
            case "4" -> "▲ 상한가";
            case "5" -> "▼ 하한가";
            default -> "보합 (" + sign + ")";
        };
    }
    
    /**
     * API 설정 정보 로깅
     */
    private void logApiConfiguration() {
        String appKey = kisApiProperties.appKey();
        String baseUrl = kisApiProperties.baseUrl();
        
        // AppKey 마스킹 (보안을 위해 마지막 4자리만 표시)
        String maskedAppKey = appKey != null && appKey.length() > 4 ? 
            "*".repeat(appKey.length() - 4) + appKey.substring(appKey.length() - 4) : 
            "*".repeat(appKey != null ? appKey.length() : 0);
            
        log.info("========== KIS API 설정 정보 ==========");
        log.info("Base URL: {}", baseUrl);
        log.info("App Key: {}", maskedAppKey);
        log.info("Account Number: {}", kisApiProperties.accountNumber());
        log.info("=====================================");
    }
}
