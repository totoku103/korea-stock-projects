package com.stock.batch.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 매 1분마다 배치 Job 실행 스케줄러
 */
@Configuration
@EnableScheduling
public class StockPriceMinuteScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockPriceMinuteScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job stockPriceMinuteJob;

    public StockPriceMinuteScheduler(JobLauncher jobLauncher, Job stockPriceMinuteJob) {
        this.jobLauncher = jobLauncher;
        this.stockPriceMinuteJob = stockPriceMinuteJob;
    }

    // 초(0)마다 1분 간격 실행
    @Scheduled(cron = "0 * * * * *")
    public void runStockPriceMinuteJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis()) // RunId 대체 키
                .toJobParameters();

            log.info("[스케줄러] 1분 주기 배치 시작");
            jobLauncher.run(stockPriceMinuteJob, params);
        } catch (Exception e) {
            log.error("[스케줄러] 1분 주기 배치 실행 실패", e);
        }
    }
}
