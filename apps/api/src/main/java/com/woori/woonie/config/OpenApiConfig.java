package com.woori.woonie.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI woonieOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Woonie Subscription Wallet API")
                .description("구독 전용 지갑, Mock Face ID, Saga 워크플로 MVP API")
                .version("0.1.0"));
    }
}
