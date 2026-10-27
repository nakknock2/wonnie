package com.woori.wonnie.finance;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "finance_alternative")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FinanceAlternative {
    @Id
    private String id;
    private String category;
    private String region;
    private String name;
    @Column(precision = 15, scale = 4)
    private BigDecimal monthlyCost;
    private String conditions;
}
