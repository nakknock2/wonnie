package com.woori.wonnie.finance;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="finance_cohort_benchmark") @Getter @AllArgsConstructor @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class FinanceCohortBenchmark {
    @Id private String id;
    private int minimumAge;
    private int maximumAge;
    private String region;
    @Column(precision=15,scale=4) private BigDecimal monthlySubscriptionAverage;
    private int sampleCount;
}
