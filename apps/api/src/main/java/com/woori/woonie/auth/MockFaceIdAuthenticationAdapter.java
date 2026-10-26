package com.woori.woonie.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "mock-face-id")
public class MockFaceIdAuthenticationAdapter implements StepUpAuthenticationPort {

    private final AuthChallengeRepository repository;

    public MockFaceIdAuthenticationAdapter(AuthChallengeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AuthChallenge createChallenge(String actionPlanId) {
        var challenge = new AuthChallenge(
                UUID.randomUUID().toString(),
                actionPlanId,
                UUID.randomUUID().toString().replace("-", "")
        );
        return repository.save(challenge);
    }

    @Override
    @Transactional
    public AuthChallenge verify(String challengeId, String challengeValue) {
        var challenge = repository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청을 찾을 수 없습니다."));
        challenge.verify(challengeValue);
        return challenge;
    }
}
