package com.woori.wonnie.assistant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AssistantMessageRepository extends JpaRepository<AssistantMessage, String> {
    List<AssistantMessage> findAllByConversationIdOrderByCreatedAtAsc(String conversationId);

    List<AssistantMessage> findTop20ByConversationIdOrderByCreatedAtDesc(String conversationId);

    long countByConversationIdAndRoleAndReadAtIsNull(String conversationId, String role);

    boolean existsBySourceKey(String sourceKey);

    @Modifying
    @Query("update AssistantMessage m set m.readAt = :readAt " +
            "where m.conversationId = :conversationId and m.role = 'ASSISTANT' " +
            "and m.readAt is null")
    int markAssistantMessagesRead(@Param("conversationId") String conversationId,
                                  @Param("readAt") LocalDateTime readAt);
}
