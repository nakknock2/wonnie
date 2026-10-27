package com.woori.wonnie.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.RedisSessionRepository;

@Configuration
@Profile("google-oidc")
public class RedisSessionStartupVerifier {
    private static final Logger log = LoggerFactory.getLogger(RedisSessionStartupVerifier.class);

    public RedisSessionStartupVerifier(SessionRepository<?> sessionRepository) {
        if (!(sessionRepository instanceof RedisSessionRepository)) {
            throw new IllegalStateException("Google 로그인 세션은 RedisSessionRepository를 사용해야 합니다.");
        }
        log.info("HTTP sessions are backed by RedisSessionRepository");
    }
}
