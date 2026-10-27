package com.woori.wonnie.finance;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@org.hibernate.annotations.Immutable
@org.hibernate.annotations.Subselect("""
    SELECT m.id,m.raw_name AS merchant_label,COALESCE(m.display_name,m.raw_name) AS display_name,
    m.candidate_services AS possible_services,m.is_aggregator AS ambiguous,m.verification_hint FROM merchant m
    """)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MerchantDirectory {
    @Id
    private String id;
    private String merchantLabel;
    private String displayName;
    private String possibleServices;
    private boolean ambiguous;
    private String verificationHint;
}
