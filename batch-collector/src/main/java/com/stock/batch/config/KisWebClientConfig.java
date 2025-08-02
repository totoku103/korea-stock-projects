package com.stock.batch.config;

import com.stock.common.config.KisApiProperties;
import com.stock.common.constants.KisApiConstants;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(KisApiProperties.class)
public class KisWebClientConfig {
    
    private final KisApiProperties kisApiProperties;
    private final ExchangeFilterFunction loggingFilter;
    private final ExchangeFilterFunction errorHandlingFilter;
    private final ExchangeFilterFunction rateLimitFilter;
    
    public KisWebClientConfig(KisApiProperties kisApiProperties,
                             ExchangeFilterFunction loggingFilter,
                             ExchangeFilterFunction errorHandlingFilter,
                             ExchangeFilterFunction rateLimitFilter) {
        this.kisApiProperties = kisApiProperties;
        this.loggingFilter = loggingFilter;
        this.errorHandlingFilter = errorHandlingFilter;
        this.rateLimitFilter = rateLimitFilter;
    }
    
    @Bean
    public WebClient kisApiWebClient() {
        // HTTP Client 설정
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, kisApiProperties.timeout().connectionTimeoutMs())
            .responseTimeout(Duration.ofMillis(kisApiProperties.timeout().readTimeoutMs()))
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(kisApiProperties.timeout().readTimeoutMs(), TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(kisApiProperties.timeout().writeTimeoutMs(), TimeUnit.MILLISECONDS)));
        
        // Exchange Strategies 설정 (메모리 버퍼 크기 증가)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
            .build();
        
        // Base URL 결정
        String baseUrl = determineBaseUrl();
        
        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .defaultHeaders(this::setDefaultHeaders)
            .filters(filters -> {
                filters.add(loggingFilter);
                filters.add(errorHandlingFilter);
                filters.add(rateLimitFilter);
            })
            .build();
    }
    
    private String determineBaseUrl() {
        if (kisApiProperties.baseUrl() != null && !kisApiProperties.baseUrl().isBlank()) {
            return kisApiProperties.baseUrl();
        }
        
        return kisApiProperties.isRealEnvironment() 
            ? KisApiConstants.REAL_BASE_URL 
            : KisApiConstants.MOCK_BASE_URL;
    }
    
    private void setDefaultHeaders(HttpHeaders headers) {
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.USER_AGENT, "korea-stock-projects/1.0.0");
        
        // API Key를 기본 헤더에 추가
        headers.add(KisApiConstants.HEADER_APP_KEY, kisApiProperties.appKey());
        headers.add(KisApiConstants.HEADER_APP_SECRET, kisApiProperties.appSecret());
        headers.add(KisApiConstants.HEADER_CUSTTYPE, KisApiConstants.CUST_TYPE_PERSONAL);
    }
}