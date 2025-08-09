
package com.stock.batch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.common.dto.KisRealTimeExecutionResponse;
import com.stock.common.dto.KisRealTimeSubscriptionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public class RealTimeExecutionWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RealTimeExecutionWebSocketHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String approvalKey;
    private final String stockCode;

    public RealTimeExecutionWebSocketHandler(String approvalKey, String stockCode) {
        this.approvalKey = approvalKey;
        this.stockCode = stockCode;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.send(Mono.fromCallable(() -> {
                KisRealTimeSubscriptionRequest subscriptionRequest = KisRealTimeSubscriptionRequest.execution(approvalKey, stockCode);
                String jsonRequest = objectMapper.writeValueAsString(subscriptionRequest);
                log.info("Sending subscription request: {}", jsonRequest);
                return session.textMessage(jsonRequest);
            }))
            .thenMany(session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(this::processMessage))
            .then();
    }

    private Mono<Void> processMessage(String payload) {
        return Mono.fromRunnable(() -> {
            if (payload.startsWith("{")) { // JSON
                try {
                    KisRealTimeExecutionResponse response = objectMapper.readValue(payload, KisRealTimeExecutionResponse.class);
                    
                    String transactionId = response.header().transactionId();
                    
                    if ("PINGPONG".equals(transactionId)) {
                        log.debug("Received PINGPONG message");
                    } else if ("H0STCNT0".equals(transactionId)) {
                        if (response.isSuccessful()) {
                            log.info("Subscription successful for stock: {}", stockCode);
                        } else {
                            log.warn("Subscription failed: {}", response.body().message());
                        }
                    } else {
                        // Actual execution data
                        if (response.isSuccessful() && response.body().output() != null) {
                            var output = response.body().output();
                            // Only log if it's actual execution data (has price field)
                            if (output.price() != null) {
                                log.info("Received execution data for {}: price={}, volume={}", 
                                    stockCode, output.price(), output.executionVolume());
                            }
                        } else {
                            log.warn("Received non-successful execution message: {}", payload);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error parsing execution data: {}", payload, e);
                }
            } else { // PONG or other non-JSON messages
                log.debug("Received non-JSON message: {}", payload);
            }
        });
    }
}
