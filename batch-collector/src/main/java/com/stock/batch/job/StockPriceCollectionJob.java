package com.stock.batch.job;

import com.stock.batch.service.StockPriceCollectionService;
import com.stock.common.dto.KisStockPriceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 주식현재가 시세 수집 배치 작업
 * 주요 종목들의 현재가 정보를 주기적으로 수집
 */
@Component
@ConditionalOnProperty(name = "batch.stock-price.enabled", havingValue = "true", matchIfMissing = false)
public class StockPriceCollectionJob implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(StockPriceCollectionJob.class);
    
    private final StockPriceCollectionService stockPriceCollectionService;

    public StockPriceCollectionJob(StockPriceCollectionService stockPriceCollectionService) {
        this.stockPriceCollectionService = stockPriceCollectionService;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("=== 주식현재가 시세 수집 배치 작업 시작 ===");
        
        try {
            // 주요 종목들의 현재가 정보 수집
            stockPriceCollectionService.collectMajorStockPrices()
                .doOnNext(this::logStockPriceInfo)
                .doOnError(error -> logger.error("배치 작업 중 오류 발생", error))
                .blockLast(); // 모든 수집이 완료될 때까지 대기
                
            logger.info("=== 주식현재가 시세 수집 배치 작업 완료 ===");
            
        } catch (Exception e) {
            logger.error("주식현재가 시세 수집 배치 작업 실패", e);
            throw e;
        }
    }
    
    /**
     * 수집된 주식 현재가 정보 로깅
     */
    private void logStockPriceInfo(KisStockPriceResponse response) {
        if (response.isSuccessful()) {
            var output = response.output();
            logger.info("📈 {} [{}]: {}원 (전일대비 {} {}%)", 
                output.stockName(),
                output.stockCode(),
                formatPrice(output.currentPrice()),
                output.priceChange(),
                output.priceChangeRate());
        }
    }
    
    /**
     * 가격 포맷팅 (천단위 콤마 추가)
     */
    private String formatPrice(String price) {
        try {
            return String.format("%,d", Integer.parseInt(price));
        } catch (NumberFormatException e) {
            return price;
        }
    }
}