package com.woori.wonnie.subscription;

import jakarta.persistence.*;
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
    private String cardId;
    private String merchantId;
    @Column(name = "service_id")
    private String serviceId;
    @Column(name = "display_name")
    private String name;
    private String category;
    @Column(precision = 15, scale = 2)
    private BigDecimal amount;
    private String billingCycle;
    @Column(name = "next_charge_on")
    private LocalDate nextPaymentDate;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    private SubscriptionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", insertable = false, updatable = false)
    private ServiceCatalog service;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BigDecimal monthlyEquivalent() {
        return "YEARLY".equals(billingCycle)
            ? amount.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP) : amount;
    }

    public void updateAmount(BigDecimal newAmount) {
        amount = newAmount;
        updatedAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
    }

    public String getBrandColor() { return service == null || service.getBrandColor() == null ? "#2789ec" : service.getBrandColor(); }
    public String getInitialText() { return service == null || service.getInitialText() == null ? name.substring(0, 1) : service.getInitialText(); }
    public String getBillingCycle() { return "YEARLY".equals(billingCycle) ? "매년" : "매월"; }
}
