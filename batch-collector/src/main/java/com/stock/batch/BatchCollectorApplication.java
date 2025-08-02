package com.stock.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "com.stock.common.entity")
public class BatchCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchCollectorApplication.class, args);
    }
}