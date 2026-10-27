package com.woori.wonnie.finance;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "finance_card")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FinanceCard {
    @Id
    private String id;
    private String customerId;
    private String productName;
    private String issuer;
    private boolean owned;
    private String benefitCategory;
    @Column(precision = 15, scale = 4)
    private BigDecimal previousMonthSpend;
    @Column(precision = 15, scale = 4)
    private BigDecimal minimumSpend;
    @Column(precision = 15, scale = 4)
    private BigDecimal discountRate;
    @Column(precision = 15, scale = 4)
    private BigDecimal monthlyCap;
    @Column(precision = 15, scale = 4)
    private BigDecimal annualFee;
    @Column(precision = 15, scale = 4)
    private BigDecimal availableLimit;
    @Column(precision = 15, scale = 4)
    private BigDecimal outstanding;
    @Column(precision = 15, scale = 4)
    private BigDecimal revolvingBalance;
    @Column(precision = 15, scale = 4)
    private BigDecimal revolvingApr;
    private String cardStatus;
    private LocalDate expiresOn;
    private LocalDate paymentOn;
    @Column(precision = 15, scale = 4)
    private BigDecimal pointsBalance;
    private LocalDate pointsExpireOn;
    private LocalDate annualFeeOn;
    private int minimumAge;
    private int maximumAge;
    private String requiredRegion;
    private String requiredEmployment;
}
