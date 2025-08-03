
package com.stock.batch.service;

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
    private static final String KIS_WEBSOCKET_URL = "ws://ops.koreainvestment.com:21000";

    private final WebSocketClient webSocketClient;
    private final KisWebSocketKeyService kisWebSocketKeyService;

    public KisRealTimeExecutionService(WebSocketClient webSocketClient, KisWebSocketKeyService kisWebSocketKeyService) {
        this.webSocketClient = webSocketClient;
        this.kisWebSocketKeyService = kisWebSocketKeyService;
    }

    public void subscribe(String stockCode) {
        kisWebSocketKeyService.getWebSocketApprovalKey()
            .flatMap(keyResponse -> {
                if (keyResponse.isSuccessful()) {
                    String approvalKey = keyResponse.approvalKey();
                    RealTimeExecutionWebSocketHandler handler = new RealTimeExecutionWebSocketHandler(approvalKey, stockCode);
                    return webSocketClient.execute(URI.create(KIS_WEBSOCKET_URL), handler);
                } else {
                    return Mono.error(new RuntimeException("Failed to get approval key: " + keyResponse.message()));
                }
            })
            .doOnError(error -> log.error("Error in real-time execution subscription for stock {}: {}", stockCode, error.getMessage()))
            .subscribe();
    }
}
