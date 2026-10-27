package com.woori.wonnie.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionPriceObservationRepository extends JpaRepository<SubscriptionPriceObservation, String> {
    boolean existsByEventId(String eventId);

    java.util.List<SubscriptionPriceObservation> findAllBySubscriptionIdIn(java.util.Collection<String> subscriptionIds);

    Optional<SubscriptionPriceObservation> findFirstBySubscriptionIdAndAppliedTrueOrderByObservedAtDesc(
            String subscriptionId);
}
