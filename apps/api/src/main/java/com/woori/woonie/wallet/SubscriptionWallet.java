package com.woori.woonie.wallet;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "subscription_wallet")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionWallet {

    @Id
    private String id;
    private String subscriptionId;
    private BigDecimal balance;
    private boolean autoFundingEnabled;

    @Enumerated(EnumType.STRING)
    private WalletStatus status;

    private String virtualCardLast4;

    @Enumerated(EnumType.STRING)
    private VirtualCardStatus virtualCardStatus;

    private BigDecimal monthlyLimit;

    @Version
    private long version;

    private LocalDateTime updatedAt;

    public void pauseFunding() {
        autoFundingEnabled = false;
        balance = BigDecimal.ZERO;
        status = WalletStatus.PAUSED;
        updatedAt = LocalDateTime.now();
    }

    public void pauseVirtualCard() {
        virtualCardStatus = VirtualCardStatus.PAUSED;
        updatedAt = LocalDateTime.now();
    }

    public void resume() {
        autoFundingEnabled = true;
        status = WalletStatus.ACTIVE;
        virtualCardStatus = VirtualCardStatus.ACTIVE;
        updatedAt = LocalDateTime.now();
    }
}
