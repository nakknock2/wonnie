package com.woori.woonie.saga;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PaymentPauseRepository extends JpaRepository<PaymentPause, String> {
    List<PaymentPause> findAllByStatusAndStartsAtLessThanEqual(
            PaymentPauseStatus status,
            LocalDate startsAt
    );

    List<PaymentPause> findAllByStatusAndEndsAtBefore(
            PaymentPauseStatus status,
            LocalDate endsAt
    );
}
