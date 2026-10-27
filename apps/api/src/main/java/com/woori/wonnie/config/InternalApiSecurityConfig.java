package com.woori.wonnie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
public class InternalApiSecurityConfig {
    @Bean
    @Order(0)
    SecurityFilterChain internalApiSecurityFilterChain(HttpSecurity http,
            InternalApiAuthenticationFilter authenticationFilter) throws Exception {
        return http
                .securityMatcher("/api/internal/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().hasRole("INTERNAL"))
                .addFilterBefore(authenticationFilter, AnonymousAuthenticationFilter.class)
                .build();
    }
}
