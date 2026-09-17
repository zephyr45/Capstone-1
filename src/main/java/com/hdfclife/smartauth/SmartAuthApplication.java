package com.hdfclife.smartauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartAuthApplication.class, args);
    }
}
