package com.woori.wonnie.auth;

import com.woori.wonnie.dashboard.DashboardService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentCustomer {
    private final GoogleCustomerProvisioningService provisioning;

    public CurrentCustomer(GoogleCustomerProvisioningService provisioning) {
        this.provisioning = provisioning;
    }

    public String id() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken oauth && authentication.isAuthenticated()) {
            return provisioning.findCustomerId(oauth)
                    .orElseGet(() -> provisioning.provisionAndRefresh(oauth));
        }
        return DashboardService.DEMO_CUSTOMER_ID;
    }
}
