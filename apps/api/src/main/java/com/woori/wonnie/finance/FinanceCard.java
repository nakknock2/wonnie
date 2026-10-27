package com.woori.wonnie.finance;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@org.hibernate.annotations.Immutable
@org.hibernate.annotations.Subselect("""
        SELECT c.id,c.customer_id,p.name AS product_name,p.issuer_name AS issuer,TRUE AS owned,'ALL' AS benefit_category,
        COALESCE(st.spend_amount,0) AS previous_month_spend,
        COALESCE((SELECT MIN(b.min_prev_month_spend) FROM card_product_benefit b WHERE b.card_product_id=p.id),0) AS minimum_spend,
        COALESCE((SELECT MAX(b.discount_rate/100) FROM card_product_benefit b WHERE b.card_product_id=p.id),0) AS discount_rate,
        COALESCE((SELECT MAX(b.monthly_cap) FROM card_product_benefit b WHERE b.card_product_id=p.id),0) AS monthly_cap,
        p.annual_fee,c.available_limit,c.outstanding_amount AS outstanding,c.revolving_balance,c.revolving_apr,
        c.status AS card_status,c.expires_on,w.balance AS points_balance,w.expiring_on AS points_expire_on,
        w.expiring_amount AS expiring_points,p.minimum_age,p.maximum_age,p.required_region_sido AS required_region,
        p.required_occupation AS required_employment,p.id AS product_id,p.card_type,c.billing_day,c.annual_fee_month,dc.today AS business_date,
        COALESCE(cur.benefit_discount_used,0) AS benefit_discount_used
        FROM card c JOIN card_product p ON p.id=c.card_product_id JOIN demo_clock dc ON dc.id=1
        LEFT JOIN point_wallet w ON w.customer_id=c.customer_id
        LEFT JOIN card_monthly_stat st ON st.card_id=c.id AND st.ym=CONCAT(
          CASE WHEN EXTRACT(MONTH FROM dc.today)=1 THEN EXTRACT(YEAR FROM dc.today)-1 ELSE EXTRACT(YEAR FROM dc.today) END,
          '-',LPAD(CONCAT('',CASE WHEN EXTRACT(MONTH FROM dc.today)=1 THEN 12 ELSE EXTRACT(MONTH FROM dc.today)-1 END),2,'0'))
        LEFT JOIN card_monthly_stat cur ON cur.card_id=c.id AND cur.ym=CONCAT(EXTRACT(YEAR FROM dc.today),'-',LPAD(CONCAT('',EXTRACT(MONTH FROM dc.today)),2,'0'))
        UNION ALL
        SELECT p.id,c.id,p.name,p.issuer_name,FALSE,'ALL',0,
        COALESCE((SELECT MIN(b.min_prev_month_spend) FROM card_product_benefit b WHERE b.card_product_id=p.id),0),
        COALESCE((SELECT MAX(b.discount_rate/100) FROM card_product_benefit b WHERE b.card_product_id=p.id),0),
        COALESCE((SELECT MAX(b.monthly_cap) FROM card_product_benefit b WHERE b.card_product_id=p.id),0),
        p.annual_fee,NULL,NULL,NULL,NULL,'CANDIDATE',NULL,NULL,NULL,NULL,
        p.minimum_age,p.maximum_age,p.required_region_sido,p.required_occupation,p.id,p.card_type,NULL,NULL,dc.today,0
        FROM customer c CROSS JOIN card_product p JOIN demo_clock dc ON dc.id=1
        WHERE NOT EXISTS (SELECT 1 FROM card held WHERE held.customer_id=c.id AND held.card_product_id=p.id)
        """)
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

    @Column(precision = 15, scale = 4)
    private BigDecimal pointsBalance;
    private LocalDate pointsExpireOn;

    private Integer minimumAge;
    private Integer maximumAge;
    private String requiredRegion;
    private String requiredEmployment;
    private String productId;
    private String cardType;
    private Integer billingDay;
    private Integer annualFeeMonth;
    private LocalDate businessDate;
    private BigDecimal expiringPoints;
    private BigDecimal benefitDiscountUsed;

    public LocalDate getPaymentOn() {
        if (billingDay == null) return null;
        var month=java.time.YearMonth.from(businessDate);
        var date=month.atDay(Math.min(billingDay,month.lengthOfMonth()));
        if (date.isBefore(businessDate)) { month=month.plusMonths(1); date=month.atDay(Math.min(billingDay,month.lengthOfMonth())); }
        return date;
    }
    public LocalDate getAnnualFeeOn() {
        if (annualFeeMonth == null) return null;
        var date=java.time.LocalDate.of(businessDate.getYear(),annualFeeMonth,1);
        return date.isBefore(businessDate) ? date.plusYears(1) : date;
    }
}
