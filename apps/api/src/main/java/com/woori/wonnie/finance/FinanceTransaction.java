package com.woori.wonnie.finance;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@org.hibernate.annotations.Immutable
@org.hibernate.annotations.Subselect("""
    SELECT t.id,t.customer_id,t.card_id,t.subscription_id,t.merchant_name_raw AS merchant_label,
    CASE WHEN t.category='TELECOM' THEN 'LIVING' ELSE t.category END AS category,
    t.amount,t.original_amount,t.currency,t.exchange_rate,t.approved_at AS authorized_at,
    t.status AS transaction_status,t.decline_reason FROM card_transaction t
    """)
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
    public LocalDateTime getAuthorizedAt() {
        return authorizedAt.atOffset(java.time.ZoneOffset.UTC).atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }
    private String transactionStatus;
    private String declineReason;
}
