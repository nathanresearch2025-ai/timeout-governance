package com.enterprise.timeout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TimeoutGovernanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimeoutGovernanceApplication.class, args);
    }
}
