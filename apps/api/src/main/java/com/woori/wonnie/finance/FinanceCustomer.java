package com.woori.wonnie.finance;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@org.hibernate.annotations.Immutable
@org.hibernate.annotations.Subselect("""
    SELECT c.id,c.birth_date,c.region_sido AS region,c.occupation AS employment,c.monthly_income,
    (SELECT SUM(a.balance) FROM bank_account a WHERE a.customer_id=c.id AND a.account_type='CHECKING') AS account_balance,
    COALESCE(p.notify_start_hour,13) AS preferred_notice_hour
    FROM customer c LEFT JOIN customer_preference p ON p.customer_id=c.id
    """)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FinanceCustomer {
    @Id
    private String id;
    private LocalDate birthDate;
    private String region;
    private String employment;
    @Column(precision = 15, scale = 4)
    private BigDecimal monthlyIncome;
    @Column(precision = 15, scale = 4)
    private BigDecimal accountBalance;
    private int preferredNoticeHour;
}
