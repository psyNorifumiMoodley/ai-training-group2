package com.psybergate.dap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DapApplication {

    public static void main(String[] args) {
        SpringApplication.run(DapApplication.class, args);
    }
}
