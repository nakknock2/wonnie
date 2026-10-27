package com.woori.wonnie.subscription;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Entity
@Table(name = "subscription")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    private String id;
    private String customerId;
    private String name;
    private String category;
    private BigDecimal amount;
    private String billingCycle;
    private LocalDate nextPaymentDate;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    private String brandColor;
    private String initialText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BigDecimal monthlyEquivalent() {
        return billingCycle.contains("연") || billingCycle.contains("년")
            ? amount.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP) : amount;
    }

    public void updateAmount(BigDecimal newAmount) {
        amount = newAmount;
        updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public static Subscription membershipDemo(String customerId, LocalDate today) {
        var subscription=cancelledDemo("sub-gym", customerId, today);
        subscription.name="데모 피트니스 연간권";
        subscription.category="운동";
        subscription.amount=new BigDecimal("600000");
        subscription.billingCycle="매년";
        subscription.nextPaymentDate=today.plusDays(20);
        subscription.status=SubscriptionStatus.ACTIVE;
        subscription.initialText="G";
        return subscription;
    }

    public static Subscription cancelledDemo(String id, String customerId, LocalDate today) {
        var subscription = new Subscription();
        subscription.id = id;
        subscription.customerId = customerId;
        subscription.name = "해지 확인된 데모 스트리밍";
        subscription.category = "영상";
        subscription.amount = new BigDecimal("9900");
        subscription.billingCycle = "매월";
        subscription.nextPaymentDate = today.plusMonths(1);
        subscription.status = SubscriptionStatus.CANCELLED;
        subscription.brandColor = "#777777";
        subscription.initialText = "D";
        subscription.createdAt = today.atStartOfDay();
        subscription.updatedAt = today.atStartOfDay();
        return subscription;
    }
}
