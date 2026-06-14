package com.sportexpo.exhibitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ExhibitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExhibitorApplication.class, args);
    }
}
