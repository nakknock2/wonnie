package com.woori.woonie.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    List<Subscription> findAllByCustomerIdOrderByNextPaymentDateAsc(String customerId);
}
