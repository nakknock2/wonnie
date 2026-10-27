package com.woori.wonnie.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "oauth_identity", uniqueConstraints = {
        @UniqueConstraint(name = "uq_oauth_identity_provider_subject", columnNames = {"provider", "provider_subject"}),
        @UniqueConstraint(name = "uq_oauth_identity_customer_provider", columnNames = {"customer_id", "provider"})
})
public class OAuthIdentity {
    @Id
    private String id;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(length = 320)
    private String email;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "picture_url", length = 1000)
    private String pictureUrl;

    @Column(name = "last_login_at", nullable = false)
    private LocalDateTime lastLoginAt;

    protected OAuthIdentity() {
    }

    private OAuthIdentity(String id, String provider, String providerSubject, String customerId,
            String email, Boolean emailVerified, String pictureUrl) {
        this.id = id;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.customerId = customerId;
        this.email = email;
        this.emailVerified = emailVerified;
        this.pictureUrl = pictureUrl;
        this.lastLoginAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    static OAuthIdentity create(String id, String provider, String providerSubject, String customerId,
            String email, Boolean emailVerified, String pictureUrl) {
        return new OAuthIdentity(id, provider, providerSubject, customerId, email, emailVerified, pictureUrl);
    }

    void refresh(String email, Boolean emailVerified, String pictureUrl) {
        this.email = email;
        this.emailVerified = emailVerified;
        this.pictureUrl = pictureUrl;
        this.lastLoginAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }
}
