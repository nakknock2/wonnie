package com.woori.woonie.auth;

public interface StepUpAuthenticationPort {
    AuthChallenge createChallenge(String actionPlanId);

    AuthChallenge verify(String challengeId, String challengeValue);
}
