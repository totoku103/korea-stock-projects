package com.stock.batch.job;

import com.stock.batch.service.StockPriceCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 1분 주기 수집용 배치 Job/Step 설정
 */
@Configuration
public class StockPriceMinuteJobConfig {

    private static final Logger log = LoggerFactory.getLogger(StockPriceMinuteJobConfig.class);

    @Bean
    public Step stockPriceMinuteStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     StockPriceCollectionService stockPriceCollectionService) {
        return new StepBuilder("stockPriceMinuteStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("[배치] 1분 주기 종목 현재가 수집 Step 시작");
                try {
                    // 주요 종목 현재가를 순차 수집 (Reactive -> 블로킹 완료 대기)
                    stockPriceCollectionService
                        .collectMajorStockPrices()
                        .collectList()
                        .block();
                    log.info("[배치] 1분 주기 종목 현재가 수집 Step 완료");
                } catch (Exception e) {
                    log.error("[배치] 1분 주기 종목 현재가 수집 Step 중 오류", e);
                    throw e;
                }
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

    @Bean
    public Job stockPriceMinuteJob(JobRepository jobRepository,
                                   Step stockPriceMinuteStep) {
        return new JobBuilder("stockPriceMinuteJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(stockPriceMinuteStep)
            .build();
    }
}
