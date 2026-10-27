package com.woori.wonnie.finance;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name="card_product_benefit") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class CardProductBenefit {
    @Id private String id;
    private String cardProductId;
    private String serviceId;
    private String benefitArea;
    private BigDecimal discountRate;
    private BigDecimal monthlyCap;
    private BigDecimal minPrevMonthSpend;
}
