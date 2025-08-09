
package com.stock.batch.config;

import com.stock.common.config.KisWebClientConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebSocketClientConfig {

    @Bean
    public WebClient webSocketClient(KisWebClientConfig kisWebClientConfig) {
        return kisWebClientConfig.kisApiWebClient();
    }
}
