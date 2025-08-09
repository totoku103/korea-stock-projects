package com.stock.batch.service;

import com.stock.common.dto.KisStockPriceRequest;
import com.stock.common.dto.KisStockPriceResponse;
import com.stock.common.service.KisApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * 주식현재가 시세 수집 서비스
 * 배치 작업을 통해 주기적으로 주식 현재가 정보를 수집
 */
@Service
public class StockPriceCollectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(StockPriceCollectionService.class);
    
    private final KisApiService kisApiService;
    
    // 주요 종목들 (예시)
    private static final List<KisStockPriceRequest> SAMPLE_STOCKS = List.of(
        KisStockPriceRequest.kospi("005930"), // 삼성전자
        KisStockPriceRequest.kospi("000660"), // SK하이닉스  
        KisStockPriceRequest.kospi("035420"), // NAVER
        KisStockPriceRequest.kospi("005490"), // POSCO홀딩스
        KisStockPriceRequest.kosdaq("035720") // 카카오
    );

    public StockPriceCollectionService(KisApiService kisApiService) {
        this.kisApiService = kisApiService;
    }

    /**
     * 단일 종목의 현재가 정보 수집
     */
    public Mono<KisStockPriceResponse> collectSingleStockPrice(String stockCode, String market) {
        KisStockPriceRequest request = new KisStockPriceRequest(stockCode, market);
        return collectSingleStockPrice(request);
    }
    
    /**
     * 단일 종목의 현재가 정보 수집
     */
    public Mono<KisStockPriceResponse> collectSingleStockPrice(KisStockPriceRequest request) {
        logger.info("종목 현재가 수집 시작: 종목코드={}, 시장={}", request.stockCode(), request.market());
        
        return kisApiService.getStockPrice(request)
            .doOnSuccess(response -> {
                if (response != null && response.isSuccessful()) {
                    var output = response.output();
                    logger.info("종목 현재가 수집 완료: 종목코드={}, 종목명={}, 현재가={}, 전일대비={}({}%)", 
                        output.stockCode(),
                        output.stockName(),
                        output.currentPrice(),
                        output.priceChange(),
                        output.priceChangeRate());
                } else {
                    logger.error("종목 현재가 수집 실패: 종목코드={}, 오류={}", 
                        request.stockCode(), response != null ? response.getErrorMessage() : "알 수 없는 오류");
                }
            })
            .doOnError(error -> {
                logger.error("종목 현재가 수집 중 예외 발생: 종목코드={}", request.stockCode(), error);
            });
    }

    /**
     * 여러 종목의 현재가 정보를 순차적으로 수집
     * API 호출 제한을 고려하여 딜레이 적용
     */
    public Flux<KisStockPriceResponse> collectMultipleStockPrices(List<KisStockPriceRequest> requests) {
        logger.info("다중 종목 현재가 수집 시작: 총 {}개 종목", requests.size());
        
        return Flux.fromIterable(requests)
            .flatMap(request -> 
                collectSingleStockPrice(request)
                    .onErrorResume(error -> {
                        logger.warn("종목 {} 현재가 수집 실패, 건너뜀", request.stockCode(), error);
                        return Mono.empty();
                    })
                    .delayElement(Duration.ofMillis(200)), // API 호출 간격 200ms
                2) // 동시 호출 제한: 2개
            .doOnComplete(() -> {
                logger.info("다중 종목 현재가 수집 완료");
            });
    }
    
    /**
     * 샘플 주요 종목들의 현재가 정보 수집
     */
    public Flux<KisStockPriceResponse> collectMajorStockPrices() {
        logger.info("주요 종목 현재가 수집 시작");
        return collectMultipleStockPrices(SAMPLE_STOCKS);
    }
    
    /**
     * 특정 종목 리스트의 현재가 정보 수집
     */
    public Flux<KisStockPriceResponse> collectStockPricesByStockCodes(List<String> stockCodes) {
        List<KisStockPriceRequest> requests = stockCodes.stream()
            .map(code -> {
                // 기본적으로 코스피로 설정, 실제로는 종목코드로 시장 구분 필요
                String market = determineMarketByStockCode(code);
                return new KisStockPriceRequest(code, market);
            })
            .toList();
            
        return collectMultipleStockPrices(requests);
    }
    
    /**
     * 종목코드로 시장 구분 결정 (간단한 휴리스틱)
     * 실제로는 데이터베이스에서 조회하거나 별도 API 호출 필요
     */
    private String determineMarketByStockCode(String stockCode) {
        // 000000-099999: 코스피
        // 100000-299999: 코스닥 (실제로는 더 복잡함)
        try {
            int code = Integer.parseInt(stockCode);
            return code < 100000 ? "J" : "Q";
        } catch (NumberFormatException e) {
            return "J"; // 기본값: 코스피
        }
    }
}