package com.kuleanasystems.assets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.kuleanasystems.assets",
        "com.kuleanasystems.security"
})
public class AssetsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetsServiceApplication.class, args);
    }
}
