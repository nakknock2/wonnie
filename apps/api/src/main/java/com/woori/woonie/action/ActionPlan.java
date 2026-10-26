package com.woori.woonie.action;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "action_plan")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActionPlan {

    @Id
    private String id;
    private String customerId;
    private String subscriptionId;
    private String actionType;
    private LocalDate startsOn;
    private LocalDate endsOn;
    private String reason;
    private String warning;

    @Enumerated(EnumType.STRING)
    private ActionPlanStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public ActionPlan(
            String id,
            String customerId,
            String subscriptionId,
            LocalDate startsOn,
            LocalDate endsOn,
            String reason,
            String warning
    ) {
        this.id = id;
        this.customerId = customerId;
        this.subscriptionId = subscriptionId;
        this.actionType = "PAUSE_SUBSCRIPTION_FUNDING";
        this.startsOn = startsOn;
        this.endsOn = endsOn;
        this.reason = reason;
        this.warning = warning;
        this.status = ActionPlanStatus.PENDING_CONFIRMATION;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusMinutes(10);
    }

    public void startAuthentication() {
        assertPending();
        status = ActionPlanStatus.AUTHENTICATING;
    }

    public void authenticate() {
        if (status != ActionPlanStatus.AUTHENTICATING) {
            throw new IllegalStateException("인증을 시작하지 않은 작업 계획입니다.");
        }
        status = ActionPlanStatus.AUTHENTICATED;
    }

    public void execute() {
        if (status != ActionPlanStatus.AUTHENTICATED) {
            throw new IllegalStateException("인증되지 않은 작업 계획입니다.");
        }
        status = ActionPlanStatus.EXECUTED;
    }

    public void assertPending() {
        if (expiresAt.isBefore(LocalDateTime.now())) {
            status = ActionPlanStatus.EXPIRED;
            throw new IllegalStateException("작업 계획이 만료됐습니다. 다시 요청해 주세요.");
        }
        if (status != ActionPlanStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException("이미 처리 중이거나 완료된 작업 계획입니다.");
        }
    }
}
