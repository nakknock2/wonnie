package com.woori.wonnie.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WonnieMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(WonnieMcpApplication.class, args);
    }
}
