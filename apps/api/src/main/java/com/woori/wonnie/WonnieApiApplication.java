package com.woori.wonnie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WonnieApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WonnieApiApplication.class, args);
	}

}
