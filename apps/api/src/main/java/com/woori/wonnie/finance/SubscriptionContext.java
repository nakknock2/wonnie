package com.woori.wonnie.finance;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_context")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SubscriptionContext {
    @Id
    private String id;
    private String cardId;
    private String serviceFamily;
    private String planName;
    private LocalDate trialEndsOn;
    private LocalDate annualRenewalOn;
    private String contractStatus;
    private LocalDate contractVerifiedOn;
    private LocalDate cancelledOn;
    private Integer usageCount30Days;
    private String usageEvidence;
    private String bundleDescription;
    private boolean familyCovered;
    @Column(precision = 15, scale = 4)
    private BigDecimal youthPlanAmount;
    @Column(precision = 15, scale = 4)
    private BigDecimal payPerUseAmount;
}
