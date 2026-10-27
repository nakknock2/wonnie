package com.woori.wonnie.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI wonnieOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Wonnie Subscription Assistant API")
                .description("구독 가격 변동 감지와 사용자별 상담 기록 API")
                .version("0.1.0"));
    }
}
