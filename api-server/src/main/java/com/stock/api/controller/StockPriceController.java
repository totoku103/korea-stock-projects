package com.stock.api.controller;

import com.stock.common.dto.ApiResponse;
import com.stock.common.dto.KisStockPriceRequest;
import com.stock.common.dto.KisStockPriceResponse;
import com.stock.common.service.KisApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 주식 시세 조회 컨트롤러
 * 한국투자증권 API를 통해 주식 현재가 정보를 제공
 */
@RestController
@RequestMapping("/api/v1/stocks")
@Tag(name = "Stock Price API", description = "주식 현재가 시세 조회 API")
public class StockPriceController {
    
    private static final Logger logger = LoggerFactory.getLogger(StockPriceController.class);
    
    private final KisApiService kisApiService;

    public StockPriceController(KisApiService kisApiService) {
        this.kisApiService = kisApiService;
    }

    /**
     * 주식 현재가 시세 조회
     */
    @GetMapping("/{stockCode}/price")
    @Operation(summary = "주식 현재가 조회", description = "특정 종목의 현재가 정보를 조회합니다.")
    public Mono<ResponseEntity<ApiResponse<KisStockPriceResponse>>> getStockPrice(
            @Parameter(description = "종목코드 (6자리)", example = "005930")
            @PathVariable String stockCode,
            @Parameter(description = "시장구분 (J: 코스피, Q: 코스닥)", example = "J")
            @RequestParam(defaultValue = "J") String market) {
        
        logger.info("주식 현재가 조회 API 호출: 종목코드={}, 시장={}", stockCode, market);
        
        try {
            KisStockPriceRequest request = new KisStockPriceRequest(stockCode, market);
            
            return kisApiService.getStockPrice(request)
                .map(response -> {
                    if (response.isSuccessful()) {
                        logger.info("주식 현재가 조회 성공: 종목코드={}, 현재가={}", 
                            stockCode, response.output().currentPrice());
                        return ResponseEntity.ok(ApiResponse.success("주식 현재가 조회 성공", response));
                    } else {
                        logger.warn("주식 현재가 조회 실패: 종목코드={}, 오류={}", 
                            stockCode, response.getErrorMessage());
                        return ResponseEntity.badRequest()
                            .body(ApiResponse.<KisStockPriceResponse>error("주식 현재가 조회 실패: " + response.getErrorMessage()));
                    }
                })
                .onErrorResume(error -> {
                    logger.error("주식 현재가 조회 중 예외 발생: 종목코드={}", stockCode, error);
                    ApiResponse<KisStockPriceResponse> errorResponse = 
                        ApiResponse.error("주식 현재가 조회 실패: " + error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
                });
                
        } catch (IllegalArgumentException e) {
            logger.warn("잘못된 요청 파라미터: 종목코드={}, 시장={}, 오류={}", stockCode, market, e.getMessage());
            ApiResponse<KisStockPriceResponse> errorResponse = ApiResponse.error("잘못된 요청: " + e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
    }

    /**
     * 코스피 종목 현재가 조회
     */
    @GetMapping("/kospi/{stockCode}/price")
    @Operation(summary = "코스피 종목 현재가 조회", description = "코스피 시장 종목의 현재가 정보를 조회합니다.")
    public Mono<ResponseEntity<ApiResponse<KisStockPriceResponse>>> getKospiStockPrice(
            @Parameter(description = "종목코드 (6자리)", example = "005930")
            @PathVariable String stockCode) {
        
        logger.info("코스피 종목 현재가 조회 API 호출: 종목코드={}", stockCode);
        
        return kisApiService.getKospiStockPrice(stockCode)
            .map(response -> {
                if (response.isSuccessful()) {
                    logger.info("코스피 종목 현재가 조회 성공: 종목코드={}, 현재가={}", 
                        stockCode, response.output().currentPrice());
                    return ResponseEntity.ok(ApiResponse.success("코스피 종목 현재가 조회 성공", response));
                } else {
                    logger.warn("코스피 종목 현재가 조회 실패: 종목코드={}, 오류={}", 
                        stockCode, response.getErrorMessage());
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.<KisStockPriceResponse>error("코스피 종목 현재가 조회 실패: " + response.getErrorMessage()));
                }
            })
            .onErrorResume(error -> {
                logger.error("코스피 종목 현재가 조회 중 예외 발생: 종목코드={}", stockCode, error);
                ApiResponse<KisStockPriceResponse> errorResponse = 
                    ApiResponse.error("코스피 종목 현재가 조회 실패: " + error.getMessage());
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }

    /**
     * 코스닥 종목 현재가 조회
     */
    @GetMapping("/kosdaq/{stockCode}/price")
    @Operation(summary = "코스닥 종목 현재가 조회", description = "코스닥 시장 종목의 현재가 정보를 조회합니다.")
    public Mono<ResponseEntity<ApiResponse<KisStockPriceResponse>>> getKosdaqStockPrice(
            @Parameter(description = "종목코드 (6자리)", example = "035720")
            @PathVariable String stockCode) {
        
        logger.info("코스닥 종목 현재가 조회 API 호출: 종목코드={}", stockCode);
        
        return kisApiService.getKosdaqStockPrice(stockCode)
            .map(response -> {
                if (response.isSuccessful()) {
                    logger.info("코스닥 종목 현재가 조회 성공: 종목코드={}, 현재가={}", 
                        stockCode, response.output().currentPrice());
                    return ResponseEntity.ok(ApiResponse.success("코스닥 종목 현재가 조회 성공", response));
                } else {
                    logger.warn("코스닥 종목 현재가 조회 실패: 종목코드={}, 오류={}", 
                        stockCode, response.getErrorMessage());
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.<KisStockPriceResponse>error("코스닥 종목 현재가 조회 실패: " + response.getErrorMessage()));
                }
            })
            .onErrorResume(error -> {
                logger.error("코스닥 종목 현재가 조회 중 예외 발생: 종목코드={}", stockCode, error);
                ApiResponse<KisStockPriceResponse> errorResponse = 
                    ApiResponse.error("코스닥 종목 현재가 조회 실패: " + error.getMessage());
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }
}