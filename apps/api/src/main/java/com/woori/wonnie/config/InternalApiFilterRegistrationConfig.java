package com.woori.wonnie.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalApiFilterRegistrationConfig {
    @Bean
    FilterRegistrationBean<InternalApiAuthenticationFilter> disableContainerRegistration(
            InternalApiAuthenticationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
