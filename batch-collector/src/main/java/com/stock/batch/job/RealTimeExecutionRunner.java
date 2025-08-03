
package com.stock.batch.job;

import com.stock.batch.service.KisRealTimeExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RealTimeExecutionRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RealTimeExecutionRunner.class);
    private final KisRealTimeExecutionService kisRealTimeExecutionService;

    public RealTimeExecutionRunner(KisRealTimeExecutionService kisRealTimeExecutionService) {
        this.kisRealTimeExecutionService = kisRealTimeExecutionService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting real-time execution subscription for Samsung Electronics (005930).");
        kisRealTimeExecutionService.subscribe("005930");
    }
}
