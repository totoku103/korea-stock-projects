package com.stock.batch.service;

import com.stock.common.dto.KisStockPriceRequest;
import com.stock.common.dto.KisStockPriceResponse;
import com.stock.common.service.KisApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockPriceCollectionServiceTest {

    @Mock
    private KisApiService kisApiService;

    private StockPriceCollectionService service;

    @BeforeEach
    void setUp() {
        service = new StockPriceCollectionService(kisApiService);
    }

    @Test
    @DisplayName("단일 종목 현재가 수집 - 요청 객체로 성공")
    void collectSingleStockPrice_withRequest_success() {
        // given
        KisStockPriceRequest request = KisStockPriceRequest.kospi("005930");
        KisStockPriceResponse response = successResponse("005930", "삼성전자", "70000", "+500", "+0.72");
        when(kisApiService.getStockPrice(request)).thenReturn(Mono.just(response));

        // when & then
        StepVerifier.create(service.collectSingleStockPrice(request))
            .expectNextMatches(r -> r.isSuccessful() &&
                "005930".equals(r.output().stockCode()) &&
                "70000".equals(r.output().currentPrice()))
            .verifyComplete();

        verify(kisApiService, times(1)).getStockPrice(request);
    }

    @Test
    @DisplayName("단일 종목 현재가 수집 - 코드/시장으로 성공")
    void collectSingleStockPrice_withCodeAndMarket_success() {
        // given
        ArgumentCaptor<KisStockPriceRequest> captor = ArgumentCaptor.forClass(KisStockPriceRequest.class);
        KisStockPriceResponse response = successResponse("000660", "SK하이닉스", "150000", "+1000", "+0.67");
        when(kisApiService.getStockPrice(any(KisStockPriceRequest.class))).thenReturn(Mono.just(response));

        // when & then
        StepVerifier.create(service.collectSingleStockPrice("000660", "J"))
            .expectNext(response)
            .verifyComplete();

        verify(kisApiService).getStockPrice(captor.capture());
        KisStockPriceRequest sent = captor.getValue();
        assertThat(sent.stockCode()).isEqualTo("000660");
        assertThat(sent.market()).isEqualTo("J");
    }

    @Test
    @DisplayName("단일 종목 현재가 수집 - 에러 전파")
    void collectSingleStockPrice_errorPropagates() {
        // given
        KisStockPriceRequest request = KisStockPriceRequest.kosdaq("035720");
        when(kisApiService.getStockPrice(request)).thenReturn(Mono.error(new RuntimeException("API 오류")));

        // when & then
        StepVerifier.create(service.collectSingleStockPrice(request))
            .expectErrorMatches(ex -> ex instanceof RuntimeException && ex.getMessage().contains("API 오류"))
            .verify();
    }

    @Test
    @DisplayName("다중 종목 현재가 수집 - 성공 항목만 방출 (가상시간)")
    void collectMultipleStockPrices_skipErrors_emitSuccessOnly() {
        // given
        KisStockPriceRequest r1 = KisStockPriceRequest.kospi("005930");
        KisStockPriceRequest r2 = KisStockPriceRequest.kospi("000660");
        KisStockPriceRequest r3 = KisStockPriceRequest.kosdaq("035720");

        KisStockPriceResponse resp1 = successResponse("005930", "삼성전자", "70000", "+500", "+0.72");
        KisStockPriceResponse resp2 = successResponse("000660", "SK하이닉스", "150000", "+1000", "+0.67");

        when(kisApiService.getStockPrice(r1)).thenReturn(Mono.just(resp1));
        when(kisApiService.getStockPrice(r2)).thenReturn(Mono.just(resp2));
        when(kisApiService.getStockPrice(r3)).thenReturn(Mono.error(new RuntimeException("API 실패")));

        // when
        Flux<KisStockPriceResponse> flux = service.collectMultipleStockPrices(List.of(r1, r2, r3));

        // then (delayElement 이 있어 충분한 가상 시간 경과 필요)
        StepVerifier.withVirtualTime(() -> flux)
            .thenAwait(Duration.ofSeconds(2))
            .expectNextMatches(r -> "005930".equals(r.output().stockCode()))
            .expectNextMatches(r -> "000660".equals(r.output().stockCode()))
            .verifyComplete();

        verify(kisApiService, times(1)).getStockPrice(r1);
        verify(kisApiService, times(1)).getStockPrice(r2);
        verify(kisApiService, times(1)).getStockPrice(r3);
    }

    @Test
    @DisplayName("종목코드 리스트로 수집 - 시장 구분 휴리스틱 적용")
    void collectStockPricesByStockCodes_marketHeuristic() {
        // given
        // 005930 -> J, 120000 -> Q
        when(kisApiService.getStockPrice(any(KisStockPriceRequest.class)))
            .thenAnswer(inv -> {
                KisStockPriceRequest req = inv.getArgument(0);
                KisStockPriceResponse r = successResponse(req.stockCode(), "N/A", "1", "+1", "+0.1");
                return Mono.just(r);
            });

        // when
        Flux<KisStockPriceResponse> flux = service.collectStockPricesByStockCodes(List.of("005930", "120000"));

        // then
        StepVerifier.withVirtualTime(() -> flux)
            .thenAwait(Duration.ofSeconds(2))
            .expectNextCount(2)
            .verifyComplete();

        ArgumentCaptor<KisStockPriceRequest> captor = ArgumentCaptor.forClass(KisStockPriceRequest.class);
        verify(kisApiService, atLeast(2)).getStockPrice(captor.capture());
        List<KisStockPriceRequest> sent = captor.getAllValues();
        assertThat(sent).extracting(KisStockPriceRequest::stockCode).contains("005930", "120000");
        // 시장 구분 확인
        assertThat(sent.stream().filter(r -> r.stockCode().equals("005930")).findFirst().orElseThrow().market())
            .isEqualTo("J");
        assertThat(sent.stream().filter(r -> r.stockCode().equals("120000")).findFirst().orElseThrow().market())
            .isEqualTo("Q");
    }

    @Test
    @DisplayName("주요 샘플 종목 수집 - 모두 호출")
    void collectMajorStockPrices_callsAllSamples() {
        // given
        when(kisApiService.getStockPrice(any(KisStockPriceRequest.class)))
            .thenAnswer(inv -> {
                KisStockPriceRequest req = inv.getArgument(0);
                return Mono.just(successResponse(req.stockCode(), "N/A", "1", "+1", "+0.1"));
            });

        // when
        Flux<KisStockPriceResponse> flux = service.collectMajorStockPrices();

        // then
        StepVerifier.withVirtualTime(() -> flux)
            .thenAwait(Duration.ofSeconds(3))
            .expectNextCount(5)
            .verifyComplete();

        verify(kisApiService, atLeast(5)).getStockPrice(any(KisStockPriceRequest.class));
    }

    private static KisStockPriceResponse successResponse(
        String stockCode,
        String stockName,
        String currentPrice,
        String priceChange,
        String priceChangeRate
    ) {
        KisStockPriceResponse.Output output = new KisStockPriceResponse.Output(
            /* currentPrice */ currentPrice,
            /* priceChange */ priceChange,
            /* priceChangeSign */ "+",
            /* priceChangeRate */ priceChangeRate,
            /* askPrice1 */ null,
            /* bidPrice1 */ null,
            /* askQuantity1 */ null,
            /* bidQuantity1 */ null,
            /* accumulatedVolume */ null,
            /* accumulatedValue */ null,
            /* sellVolume */ null,
            /* buyVolume */ null,
            /* openPrice */ null,
            /* highPrice */ null,
            /* lowPrice */ null,
            /* maxPrice */ null,
            /* minPrice */ null,
            /* stockCode */ stockCode,
            /* stockName */ stockName,
            /* marketName */ null,
            /* marketCap */ null,
            /* listedShares */ null,
            /* statusCode */ null,
            /* marginRate */ null,
            /* transactionTime */ null,
            /* viCode */ null
        );
        return new KisStockPriceResponse(
            /* returnCode */ "0",
            /* messageCode */ "0",
            /* message */ "성공",
            /* output */ output
        );
    }
}
