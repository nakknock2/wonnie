package com.woori.woonie.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionWalletRepository extends JpaRepository<SubscriptionWallet, String> {
    Optional<SubscriptionWallet> findBySubscriptionId(String subscriptionId);
}
