package com.survey.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableDiscoveryClient
@EnableRetry
@MapperScan("com.survey.ai.mapper")
public class AiOrchestratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiOrchestratorApplication.class, args);
    }
}