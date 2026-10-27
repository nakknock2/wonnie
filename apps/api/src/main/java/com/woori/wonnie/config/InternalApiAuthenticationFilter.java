package com.woori.wonnie.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class InternalApiAuthenticationFilter extends OncePerRequestFilter {
    public static final String TOKEN_HEADER = "X-Wonnie-Internal-Token";
    private final byte[] expectedToken;

    public InternalApiAuthenticationFilter(
            @Value("${app.internal-api.token:local-development-token}") String token) {
        this.expectedToken = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var supplied = request.getHeader(TOKEN_HEADER);
        if (supplied != null && !supplied.isBlank()
                && MessageDigest.isEqual(expectedToken, supplied.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(
                            "wonnie-mcp", null,
                            List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))));
        }
        filterChain.doFilter(request, response);
    }
}
