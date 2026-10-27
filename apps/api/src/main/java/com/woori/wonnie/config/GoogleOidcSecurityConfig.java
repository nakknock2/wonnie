package com.woori.wonnie.config;

import com.woori.wonnie.auth.CustomerProvisioningAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpStatus;

@Configuration
@Profile("google-oidc")
public class GoogleOidcSecurityConfig {

    private final String frontendUrl;
    private final CustomerProvisioningAuthenticationSuccessHandler successHandler;

    GoogleOidcSecurityConfig(@Value("${app.auth.frontend-url}") String frontendUrl,
            CustomerProvisioningAuthenticationSuccessHandler successHandler) {
        this.frontendUrl = frontendUrl;
        this.successHandler = successHandler;
    }

    @Bean
    OAuth2AuthorizationRequestResolver googleAuthorizationRequestResolver(
            ClientRegistrationRepository registrations
    ) {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(
                registrations, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(builder -> builder.additionalParameters(parameters -> {
            parameters.put("access_type", "offline");
            parameters.put("prompt", "consent");
            parameters.put("include_granted_scopes", "true");
        }));
        return resolver;
    }

    @Bean
    SecurityFilterChain googleOidcSecurityFilterChain(
            HttpSecurity http,
            OAuth2AuthorizationRequestResolver googleAuthorizationRequestResolver
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/logout"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**", "/api/calendar/webhook").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getRequestURI().startsWith("/api/")))
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(googleAuthorizationRequestResolver))
                        .successHandler(successHandler))
                .logout(logout -> logout.logoutSuccessUrl(frontendUrl))
                .build();
    }
}
