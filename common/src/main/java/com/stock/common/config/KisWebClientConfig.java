package com.stock.common.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(KisApiProperties.class)
public class KisWebClientConfig {
    
    private final KisApiProperties kisApiProperties;
    
    public KisWebClientConfig(KisApiProperties kisApiProperties) {
        this.kisApiProperties = kisApiProperties;
    }
    
    @Bean
    public WebClient kisWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, kisApiProperties.timeout().connectionTimeoutMs())
                .responseTimeout(Duration.ofMillis(kisApiProperties.timeout().readTimeoutMs()))
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(kisApiProperties.timeout().readTimeoutMs(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(kisApiProperties.timeout().writeTimeoutMs(), TimeUnit.MILLISECONDS)));
        
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
}