package com.woori.wonnie.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GoogleCustomerProvisioningServiceTests {
    @Autowired GoogleCustomerProvisioningService provisioning;
    @Autowired CustomerAccountRepository customers;
    @Autowired OAuthIdentityRepository identities;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc http;

    @Test
    void firstLoginCreatesOneCustomerAndRefreshKeepsTheSameCustomer() {
        var first = authentication("google-subject-1", "첫 이름", "first@example.com");
        var customerId = provisioning.provisionAndRefresh(first);

        assertThat(customerId).startsWith("cust-");
        assertThat(customers.findById(customerId)).get().extracting(CustomerAccount::getName).isEqualTo("첫 이름");
        assertThat(identities.findByProviderAndProviderSubject("google", "google-subject-1"))
                .get().extracting(OAuthIdentity::getCustomerId).isEqualTo(customerId);
        assertThat(jdbc.queryForObject("select email from oauth_identity where customer_id = ?", String.class, customerId))
                .isEqualTo("first@example.com");
        assertThat(jdbc.queryForObject("select birth_date from customer where id = ?", Object.class, customerId)).isNull();

        var refreshedId = provisioning.provisionAndRefresh(
                authentication("google-subject-1", "바뀐 이름", "changed@example.com"));

        assertThat(refreshedId).isEqualTo(customerId);
        assertThat(customers.findById(customerId)).get().extracting(CustomerAccount::getName).isEqualTo("바뀐 이름");
        assertThat(identities.count()).isEqualTo(1);
    }

    @Test
    void authenticatedApisUseTheProvisionedCustomerInsteadOfDemoData() throws Exception {
        var login = oauth2Login().attributes(attributes -> {
            attributes.put("sub", "api-user-subject");
            attributes.put("name", "새 사용자");
            attributes.put("email", "api-user@example.com");
            attributes.put("email_verified", true);
        });

        http.perform(get("/api/auth/me").with(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.startsWith("cust-")))
                .andExpect(jsonPath("$.name").value("새 사용자"));

        http.perform(get("/api/dashboard").with(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("새 사용자"))
                .andExpect(jsonPath("$.activeCount").value(0))
                .andExpect(jsonPath("$.subscriptions").isEmpty());

        assertThat(customers.count()).isEqualTo(2);
        assertThat(identities.count()).isEqualTo(1);
    }

    private static OAuth2AuthenticationToken authentication(String subject, String name, String email) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var principal = new DefaultOAuth2User(authorities, Map.of(
                "sub", subject,
                "name", name,
                "email", email,
                "email_verified", true,
                "picture", "https://example.com/picture.png"
        ), "sub");
        return new OAuth2AuthenticationToken(principal, authorities, "google");
    }
}
