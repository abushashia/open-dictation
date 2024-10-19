package com.sitedictation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
class DictationApp {

    public static void main(String[] args) {
        SpringApplication.run(DictationApp.class, args);
    }
}
