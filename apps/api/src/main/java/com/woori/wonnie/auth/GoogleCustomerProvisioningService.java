package com.woori.wonnie.auth;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleCustomerProvisioningService {
    private final CustomerAccountRepository customers;
    private final OAuthIdentityRepository identities;

    public GoogleCustomerProvisioningService(CustomerAccountRepository customers, OAuthIdentityRepository identities) {
        this.customers = customers;
        this.identities = identities;
    }

    @Transactional(readOnly = true)
    public Optional<String> findCustomerId(OAuth2AuthenticationToken authentication) {
        var provider = provider(authentication);
        var subject = requiredAttribute(authentication.getPrincipal().getAttributes(), "sub");
        return identities.findByProviderAndProviderSubject(provider, subject).map(OAuthIdentity::getCustomerId);
    }

    @Transactional
    public synchronized String provisionAndRefresh(OAuth2AuthenticationToken authentication) {
        var attributes = authentication.getPrincipal().getAttributes();
        var provider = provider(authentication);
        var subject = requiredAttribute(attributes, "sub");
        var name = limited(attribute(attributes, "name"), 50);
        if (name == null || name.isBlank()) {
            name = limited(attribute(attributes, "email"), 50);
        }
        if (name == null || name.isBlank()) {
            name = "Google 사용자";
        }
        var customerName = name;
        var email = limited(attribute(attributes, "email"), 320);
        var picture = limited(attribute(attributes, "picture"), 1000);
        var emailVerified = booleanAttribute(attributes.get("email_verified"));

        var existing = identities.findByProviderAndProviderSubject(provider, subject);
        if (existing.isPresent()) {
            var identity = existing.get();
            identity.refresh(email, emailVerified, picture);
            customers.findById(identity.getCustomerId()).ifPresent(customer -> customer.rename(customerName));
            return identity.getCustomerId();
        }

        var customerId = "cust-" + UUID.randomUUID();
        customers.save(CustomerAccount.fromGoogle(customerId, customerName));
        identities.save(OAuthIdentity.create(
                "oauth-" + UUID.randomUUID(), provider, subject, customerId, email, emailVerified, picture));
        return customerId;
    }

    private static String provider(OAuth2AuthenticationToken authentication) {
        return limited(authentication.getAuthorizedClientRegistrationId(), 30);
    }

    private static String requiredAttribute(Map<String, Object> attributes, String key) {
        var value = attribute(attributes, key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Google OIDC 응답에 " + key + "가 없습니다.");
        }
        return value;
    }

    private static String attribute(Map<String, Object> attributes, String key) {
        var value = attributes.get(key);
        return value == null ? null : String.valueOf(value).strip();
    }

    private static Boolean booleanAttribute(Object value) {
        return value == null ? null : Boolean.valueOf(String.valueOf(value));
    }

    private static String limited(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
