
package com.stock.batch.service;

import com.stock.common.config.KisApiProperties;
import com.stock.common.constants.KisApiConstants;
import com.stock.common.service.KisWebSocketKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;

@Service
public class KisRealTimeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(KisRealTimeExecutionService.class);

    private final WebSocketClient webSocketClient;
    private final KisWebSocketKeyService kisWebSocketKeyService;
    private final KisApiProperties kisApiProperties;

    public KisRealTimeExecutionService(WebSocketClient webSocketClient,
                                       KisWebSocketKeyService kisWebSocketKeyService,
                                       KisApiProperties kisApiProperties) {
        this.webSocketClient = webSocketClient;
        this.kisWebSocketKeyService = kisWebSocketKeyService;
        this.kisApiProperties = kisApiProperties;
    }

    public void subscribe(String stockCode) {
        String wsUrl = kisApiProperties.isRealEnvironment()
            ? KisApiConstants.WS_REAL_URL
            : KisApiConstants.WS_MOCK_URL;

        kisWebSocketKeyService.getWebSocketApprovalKey()
            .flatMap(keyResponse -> {
                if (keyResponse != null && keyResponse.hasApprovalKey()) {
                    String approvalKey = keyResponse.approvalKey();
                    log.info("Obtained approval key. Subscribing to {} for stock {}", wsUrl, stockCode);
                    RealTimeExecutionWebSocketHandler handler = new RealTimeExecutionWebSocketHandler(approvalKey, stockCode);
                    return webSocketClient.execute(URI.create(wsUrl), handler);
                }
                String code = keyResponse != null ? keyResponse.messageCode() : "null";
                String msg = keyResponse != null ? keyResponse.message() : "null";
                return Mono.error(new RuntimeException("Failed to get approval key: code=" + code + ", msg=" + msg));
            })
            .subscribe(
                null,
                error -> log.error("Error in real-time execution subscription for stock {}: {}", stockCode, error.getMessage())
            );
    }
}
