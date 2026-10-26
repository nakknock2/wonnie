package com.woori.woonie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WoonieApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WoonieApiApplication.class, args);
	}

}
