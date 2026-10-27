package com.woori.wonnie.finance;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "finance_transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FinanceTransaction {
    @Id
    private String id;
    private String customerId;
    private String cardId;
    private String subscriptionId;
    private String merchantLabel;
    private String category;
    @Column(precision = 15, scale = 4)
    private BigDecimal amount;
    @Column(precision = 15, scale = 4)
    private BigDecimal originalAmount;
    private String currency;
    @Column(precision = 15, scale = 4)
    private BigDecimal exchangeRate;
    private LocalDateTime authorizedAt;
    private String transactionStatus;
    private String declineReason;
}
