package com.woori.wonnie.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    List<Subscription> findAllByCustomerIdOrderByNextPaymentDateAsc(String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select subscription from Subscription subscription where subscription.id = :id")
    java.util.Optional<Subscription> findByIdForUpdate(@Param("id") String id);
}
