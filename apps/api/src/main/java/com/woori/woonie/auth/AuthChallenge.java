package com.woori.woonie.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "auth_challenge")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthChallenge {

    @Id
    private String id;
    private String actionPlanId;
    private String challengeValue;

    @Enumerated(EnumType.STRING)
    private AuthChallengeStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;

    public AuthChallenge(String id, String actionPlanId, String challengeValue) {
        this.id = id;
        this.actionPlanId = actionPlanId;
        this.challengeValue = challengeValue;
        this.status = AuthChallengeStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusMinutes(2);
    }

    public void verify(String suppliedChallenge) {
        if (status != AuthChallengeStatus.PENDING) {
            throw new IllegalStateException("이미 사용되었거나 만료된 인증 요청입니다.");
        }
        if (expiresAt.isBefore(LocalDateTime.now())) {
            status = AuthChallengeStatus.EXPIRED;
            throw new IllegalStateException("인증 요청이 만료됐습니다.");
        }
        if (!challengeValue.equals(suppliedChallenge)) {
            throw new IllegalArgumentException("인증 요청이 일치하지 않습니다.");
        }
        status = AuthChallengeStatus.VERIFIED;
        verifiedAt = LocalDateTime.now();
    }
}
