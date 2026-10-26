package com.kuleanasystems.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.kuleanasystems.tracker",
        "com.kuleanasystems.security"
})
public class TrackerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackerServiceApplication.class, args);
    }
}
