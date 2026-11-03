package com.kahua.assets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.kahua.assets", "com.kuleanasystems.security"})
public class AssetsApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssetsApplication.class, args);
	}

}
