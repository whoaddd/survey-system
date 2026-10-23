package com.survey.response;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.survey.response.mapper")
public class ResponseServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResponseServiceApplication.class, args);
    }
}