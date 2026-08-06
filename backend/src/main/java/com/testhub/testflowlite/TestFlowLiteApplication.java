package com.testhub.testflowlite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TestFlowLiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestFlowLiteApplication.class, args);
    }
}
