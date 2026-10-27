package com.woori.wonnie.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, String> {
    Optional<OAuthIdentity> findByProviderAndProviderSubject(String provider, String providerSubject);
    Optional<OAuthIdentity> findByCustomerIdAndProvider(String customerId, String provider);
    List<OAuthIdentity> findAllByProvider(String provider);
}
