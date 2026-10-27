package com.woori.wonnie.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomerProvisioningAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final GoogleCustomerProvisioningService provisioning;
    private final com.woori.wonnie.calendar.CalendarSyncService calendarSync;
    private final SimpleUrlAuthenticationSuccessHandler redirect;

    public CustomerProvisioningAuthenticationSuccessHandler(
            GoogleCustomerProvisioningService provisioning,
            com.woori.wonnie.calendar.CalendarSyncService calendarSync,
            @Value("${app.auth.frontend-url}") String frontendUrl) {
        this.provisioning = provisioning;
        this.calendarSync = calendarSync;
        this.redirect = new SimpleUrlAuthenticationSuccessHandler(frontendUrl);
        this.redirect.setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth)) {
            throw new IllegalStateException("Google OAuth2 인증 정보가 없습니다.");
        }
        var customerId = provisioning.provisionAndRefresh(oauth);
        calendarSync.schedule(customerId, oauth.getName());
        redirect.onAuthenticationSuccess(request, response, authentication);
    }
}
