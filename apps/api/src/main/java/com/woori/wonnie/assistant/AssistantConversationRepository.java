package com.woori.wonnie.assistant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssistantConversationRepository extends JpaRepository<AssistantConversation, String> {
    Optional<AssistantConversation> findByCustomerId(String customerId);
}
