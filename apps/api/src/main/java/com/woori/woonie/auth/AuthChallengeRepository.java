package com.woori.woonie.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthChallengeRepository extends JpaRepository<AuthChallenge, String> {
}
