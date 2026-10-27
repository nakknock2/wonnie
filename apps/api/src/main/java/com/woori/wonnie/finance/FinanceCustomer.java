package com.woori.wonnie.finance;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "finance_customer")
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
