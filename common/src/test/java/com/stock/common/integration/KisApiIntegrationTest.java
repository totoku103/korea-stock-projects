package com.stock.common.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.common.config.KisApiProperties;
import com.stock.common.constants.KisApiConstants;
import com.stock.common.dto.KisTokenResponse;
import com.stock.common.dto.KisWebSocketKeyResponse;
import com.stock.common.service.KisTokenService;
import com.stock.common.service.KisWebSocketKeyService;
import com.stock.common.service.KisApiService;
import com.stock.common.service.KisMockService;
import com.stock.common.exception.KisApiAuthenticationException;
import com.stock.common.dto.KisStockPriceRequest;
import com.stock.common.dto.KisStockPriceResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KisApiIntegrationTest {

    private MockWebServer mockWebServer;
    private KisTokenService kisTokenService;
    private KisWebSocketKeyService kisWebSocketKeyService;
    private KisApiService kisApiService;
    private KisMockService kisMockService;
    private KisApiProperties kisApiProperties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        kisApiProperties = new KisApiProperties(
                mockWebServer.url("/").toString(),
                "test-app-key",
                "test-app-secret",
                "01234567-89",
                "mock",
                "ws://localhost:31000",
                new KisApiProperties.RateLimit(20, 10000, 5),
                new KisApiProperties.Timeout(5000, 30000, 30000)
        );
        
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        kisTokenService = new KisTokenService(webClient, kisApiProperties);
        kisWebSocketKeyService = new KisWebSocketKeyService(webClient, kisApiProperties);
        kisApiService = new KisApiService(webClient, kisTokenService);
        kisMockService = new KisMockService();
        objectMapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }
    
    @AfterEach
    void tearDown() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }


    @Test
    void 전체_워크플로우_토큰_발급_후_웹소켓_키_발급() throws Exception {
        // Given
        // 1. 토큰 발급 응답 설정
        KisTokenResponse tokenResponse = new KisTokenResponse(
                "access-token-12345",
                "Bearer",
                86400L,
                "2024-12-31 23:59:59"
        );

        MockResponse tokenMockResponse = new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(tokenResponse));

        // 2. 웹소켓 키 발급 응답 설정
        KisWebSocketKeyResponse wsKeyResponse = new KisWebSocketKeyResponse(
                "ws-approval-key-67890",
                "0",
                "정상처리 되었습니다."
        );

        MockResponse wsKeyMockResponse = new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(wsKeyResponse));

        mockWebServer.enqueue(tokenMockResponse);
        mockWebServer.enqueue(wsKeyMockResponse);

        // When & Then
        // 1. 토큰 발급
        Mono<KisTokenResponse> tokenResult = kisTokenService.getAccessToken();
        StepVerifier.create(tokenResult)
                .expectNextMatches(response -> {
                    assertThat(response.accessToken()).isEqualTo("access-token-12345");
                    assertThat(response.isExpired()).isFalse();
                    return true;
                })
                .verifyComplete();

        // 2. 웹소켓 키 발급
        Mono<KisWebSocketKeyResponse> wsKeyResult = kisWebSocketKeyService.getWebSocketApprovalKey();
        StepVerifier.create(wsKeyResult)
                .expectNextMatches(response -> {
                    assertThat(response.approvalKey()).isEqualTo("ws-approval-key-67890");
                    assertThat(response.isSuccessful()).isTrue();
                    assertThat(response.hasApprovalKey()).isTrue();
                    return true;
                })
                .verifyComplete();

        // 3. 요청 검증
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    void 토큰_캐싱_동작_확인() throws Exception {
        // Given
        KisTokenResponse tokenResponse = new KisTokenResponse(
                "cached-token",
                "Bearer",
                86400L,
                "2024-12-31 23:59:59"
        );

        MockResponse tokenMockResponse = new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(tokenResponse));

        mockWebServer.enqueue(tokenMockResponse);

        // When & Then
        // 첫 번째 토큰 요청
        Mono<KisTokenResponse> firstResult = kisTokenService.getAccessToken();
        StepVerifier.create(firstResult)
                .expectNextMatches(response -> response.accessToken().equals("cached-token"))
                .verifyComplete();

        // 두 번째 토큰 요청 (캐시 사용)
        Mono<KisTokenResponse> secondResult = kisTokenService.getAccessToken();
        StepVerifier.create(secondResult)
                .expectNextMatches(response -> response.accessToken().equals("cached-token"))
                .verifyComplete();

        // 세 번째 토큰 요청 (캐시 사용)
        Mono<KisTokenResponse> thirdResult = kisTokenService.getAccessToken();
        StepVerifier.create(thirdResult)
                .expectNextMatches(response -> response.accessToken().equals("cached-token"))
                .verifyComplete();

        // HTTP 요청은 한 번만 발생해야 함
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        assertThat(kisTokenService.hasValidToken()).isTrue();
    }

    @Test
    void 에러_발생_시_재시도_동작_확인() throws Exception {
        // Given
        // 첫 번째, 두 번째 요청은 실패
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        // 세 번째 요청은 성공
        KisWebSocketKeyResponse successResponse = new KisWebSocketKeyResponse(
                "retry-success-key",
                "0",
                "재시도 성공"
        );

        MockResponse successMockResponse = new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(successResponse));

        mockWebServer.enqueue(successMockResponse);

        // When & Then
        Mono<KisWebSocketKeyResponse> result = kisWebSocketKeyService.getWebSocketApprovalKey();

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertThat(response.approvalKey()).isEqualTo("retry-success-key");
                    assertThat(response.isSuccessful()).isTrue();
                    return true;
                })
                .verifyComplete();

        // 3번의 요청이 발생해야 함 (2번 실패 + 1번 성공)
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void 토큰_무효화_후_새_토큰_발급() throws Exception {
        // Given
        KisTokenResponse firstToken = new KisTokenResponse(
                "first-token",
                "Bearer",
                86400L,
                "2024-12-31 23:59:59"
        );

        KisTokenResponse secondToken = new KisTokenResponse(
                "second-token",
                "Bearer",
                86400L,
                "2024-12-31 23:59:59"
        );

        // 첫 번째 토큰 발급
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(firstToken)));

        // 토큰 무효화
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // 두 번째 토큰 발급
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(secondToken)));

        // When & Then
        // 1. 첫 번째 토큰 발급
        Mono<KisTokenResponse> firstResult = kisTokenService.getAccessToken();
        StepVerifier.create(firstResult)
                .expectNextMatches(response -> response.accessToken().equals("first-token"))
                .verifyComplete();

        assertThat(kisTokenService.hasValidToken()).isTrue();

        // 2. 토큰 무효화
        Mono<Void> revokeResult = kisTokenService.revokeToken("first-token");
        StepVerifier.create(revokeResult)
                .verifyComplete();

        assertThat(kisTokenService.hasValidToken()).isFalse();

        // 3. 새 토큰 발급
        Mono<KisTokenResponse> secondResult = kisTokenService.getAccessToken();
        StepVerifier.create(secondResult)
                .expectNextMatches(response -> response.accessToken().equals("second-token"))
                .verifyComplete();

        assertThat(kisTokenService.hasValidToken()).isTrue();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void Mock_서비스_웹소켓_키_발급_테스트() {
        // Given
        KisMockService mockService = new KisMockService();
        
        // When
        Mono<KisWebSocketKeyResponse> result = mockService.getMockWebSocketApprovalKey();
        
        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertThat(response.approvalKey()).startsWith("mock-approval-key-");
                    assertThat(response.isSuccessful()).isTrue();
                    assertThat(response.messageCode()).isEqualTo("0000");
                    assertThat(response.message()).isEqualTo("SUCCESS");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void Mock_서비스_API_키_유효성_검증_테스트() {
        // Given
        KisMockService mockService = new KisMockService();
        
        // When & Then
        // 테스트 키는 유효하다고 판단
        assertThat(mockService.isValidApiKey("test-app-key", "test-app-secret")).isTrue();
        
        // 실제 형식의 키는 유효하다고 판단
        assertThat(mockService.isValidApiKey("PAxxxxxxxxxxxxxxx", "XXxxxxxxxxxxxxxxx")).isTrue();
        
        // 너무 짧거나 null인 키는 유효하지 않다고 판단
        assertThat(mockService.isValidApiKey("short", "key")).isFalse();
        assertThat(mockService.isValidApiKey(null, "test-app-secret")).isFalse();
        assertThat(mockService.isValidApiKey("test-app-key", null)).isFalse();
    }

    @Test 
    void 인증_실패_403_오류_처리_테스트() throws Exception {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error_description\":\"서버 에러가 발생했습니다.\",\"error_code\":\"EGW00002\"}"));

        // When & Then
        Mono<KisWebSocketKeyResponse> result = kisWebSocketKeyService.getWebSocketApprovalKey();
        
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> {
                    assertThat(throwable).isInstanceOf(KisApiAuthenticationException.class);
                    assertThat(throwable.getMessage()).contains("접근 권한 없음");
                    assertThat(throwable.getMessage()).contains("HTTP 403");
                    return true;
                })
                .verify();
    }

    @Test
    void 주식현재가_조회_API_테스트() throws Exception {
        // Given
        // 1. 토큰 발급 응답
        KisTokenResponse tokenResponse = new KisTokenResponse(
                "test-access-token",
                "Bearer",
                86400L,
                "2024-12-31 23:59:59"
        );
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(tokenResponse)));

        // 2. 주식현재가 응답
        KisStockPriceResponse stockPriceResponse = createMockStockPriceResponse();
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(stockPriceResponse)));

        // When
        KisStockPriceRequest request = KisStockPriceRequest.kospi("005930");
        Mono<KisStockPriceResponse> result = kisApiService.getStockPrice(request);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertThat(response.isSuccessful()).isTrue();
                    assertThat(response.output().stockCode()).isEqualTo("005930");
                    assertThat(response.output().stockName()).isEqualTo("삼성전자");
                    assertThat(response.output().currentPrice()).isEqualTo("75000");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void 주식현재가_조회_잘못된_종목코드_테스트() {
        // Given
        String invalidStockCode = "";
        
        // When & Then
        assertThatThrownBy(() -> KisStockPriceRequest.kospi(invalidStockCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종목코드는 필수입니다");
    }

    @Test
    void 주식현재가_조회_잘못된_시장구분_테스트() {
        // Given
        String stockCode = "005930";
        String invalidMarket = "X";
        
        // When & Then
        assertThatThrownBy(() -> new KisStockPriceRequest(stockCode, invalidMarket))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("시장구분은 J(코스피) 또는 Q(코스닥)만 가능합니다");
    }

    @Test
    void API_서비스_코스피_종목_조회_테스트() throws Exception {
        // Given
        KisTokenResponse tokenResponse = new KisTokenResponse(
                "test-token-kospi",
                "Bearer", 
                86400L,
                "2024-12-31 23:59:59"
        );
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(tokenResponse)));
                
        KisStockPriceResponse stockResponse = createMockStockPriceResponse();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(stockResponse)));

        // When
        Mono<KisStockPriceResponse> result = kisApiService.getKospiStockPrice("005930");

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertThat(response.isSuccessful()).isTrue();
                    assertThat(response.output().stockName()).isEqualTo("삼성전자");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void API_서비스_코스닥_종목_조회_테스트() throws Exception {
        // Given
        KisTokenResponse tokenResponse = new KisTokenResponse(
                "test-token-kosdaq",
                "Bearer",
                86400L,
                "2024-12-31 23:59:59"
        );
        
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(tokenResponse)));

        KisStockPriceResponse stockResponse = createMockKosdaqStockPriceResponse();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(stockResponse)));

        // When
        Mono<KisStockPriceResponse> result = kisApiService.getKosdaqStockPrice("035720");

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertThat(response.isSuccessful()).isTrue();
                    assertThat(response.output().stockName()).isEqualTo("카카오");
                    return true;
                })
                .verifyComplete();
    }

    private KisStockPriceResponse createMockStockPriceResponse() {
        KisStockPriceResponse.Output output = new KisStockPriceResponse.Output(
                "75000", // currentPrice
                "1000", // priceChange
                "2", // priceChangeSign (상승)
                "1.35", // priceChangeRate
                "75100", // askPrice1
                "74900", // bidPrice1
                "1000", // askQuantity1
                "1500", // bidQuantity1
                "1000000", // accumulatedVolume
                "75000000000", // accumulatedValue
                "500000", // sellVolume
                "500000", // buyVolume
                "74500", // openPrice
                "75500", // highPrice
                "74000", // lowPrice
                "97500", // maxPrice (상한가)
                "52500", // minPrice (하한가)
                "005930", // stockCode
                "삼성전자", // stockName
                "코스피", // marketName
                "450000000000000", // marketCap
                "5969782550", // listedShares
                "00", // statusCode
                "40", // marginRate
                "09:30:00", // transactionTime
                "N" // viCode
        );

        return new KisStockPriceResponse("0", "OPSP0000", "정상처리 되었습니다.", output);
    }

    private KisStockPriceResponse createMockKosdaqStockPriceResponse() {
        KisStockPriceResponse.Output output = new KisStockPriceResponse.Output(
                "55000", // currentPrice
                "-2000", // priceChange
                "5", // priceChangeSign (하락)
                "-3.51", // priceChangeRate
                "55100", // askPrice1
                "54900", // bidPrice1
                "2000", // askQuantity1
                "2500", // bidQuantity1
                "800000", // accumulatedVolume
                "44000000000", // accumulatedValue
                "400000", // sellVolume
                "400000", // buyVolume
                "57000", // openPrice
                "57200", // highPrice
                "54800", // lowPrice
                "74100", // maxPrice (상한가)
                "39900", // minPrice (하한가)
                "035720", // stockCode
                "카카오", // stockName
                "코스닥", // marketName
                "23000000000000", // marketCap
                "427643835", // listedShares
                "00", // statusCode
                "40", // marginRate
                "09:30:00", // transactionTime
                "N" // viCode
        );

        return new KisStockPriceResponse("0", "OPSP0000", "정상처리 되었습니다.", output);
    }
}