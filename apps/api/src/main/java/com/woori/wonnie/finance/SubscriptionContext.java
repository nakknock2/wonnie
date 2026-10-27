package com.woori.wonnie.finance;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@org.hibernate.annotations.Immutable
@org.hibernate.annotations.Subselect("""
    SELECT x.id,s.card_id,COALESCE(v.category,s.category) AS service_family,x.plan_name,
    x.trial_ends_on,x.trial_end_confirmed,x.annual_renewal_on,x.contract_status,x.contract_verified_on,x.cancelled_on,
    x.usage_count30_days,x.usage_evidence,x.bundle_description,x.family_covered,x.youth_plan_amount,x.pay_per_use_amount
    FROM subscription_context x JOIN subscription s ON s.id=x.id LEFT JOIN service v ON v.id=s.service_id
    """)
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
    private boolean trialEndConfirmed;
    private LocalDate annualRenewalOn;
    private String contractStatus;
    private LocalDate contractVerifiedOn;
    private LocalDate cancelledOn;
    private Integer usageCount30Days;
    private String usageEvidence;
    private String bundleDescription;
    private Boolean familyCovered;
    public boolean isFamilyCovered() { return Boolean.TRUE.equals(familyCovered); }
    @Column(precision = 15, scale = 4)
    private BigDecimal youthPlanAmount;
    @Column(precision = 15, scale = 4)
    private BigDecimal payPerUseAmount;
}
