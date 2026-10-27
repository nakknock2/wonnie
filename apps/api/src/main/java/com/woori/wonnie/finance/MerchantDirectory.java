package com.woori.wonnie.finance;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_directory")
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
