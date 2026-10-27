package com.woori.wonnie.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpStatus;

@Configuration
@Profile("google-oidc")
public class GoogleOidcSecurityConfig {

    private final String frontendUrl;

    GoogleOidcSecurityConfig(@Value("${app.auth.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Bean
    SecurityFilterChain googleOidcSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/logout"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getRequestURI().startsWith("/api/")))
                .oauth2Login(oauth -> oauth.defaultSuccessUrl(frontendUrl, true))
                .logout(logout -> logout.logoutSuccessUrl(frontendUrl))
                .build();
    }
}
