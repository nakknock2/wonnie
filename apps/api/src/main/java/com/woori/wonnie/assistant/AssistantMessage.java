package com.woori.wonnie.assistant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Getter
@Table(name = "chat_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssistantMessage {
    @Id
    private String id;

    @Column(nullable = false, length = 64)
    private String conversationId;
    @Column(nullable = false, length = 64)
    private String customerId;

    @Column(nullable = false, length = 10)
    private String role;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "source_key", unique = true, length = 180)
    private String sourceKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    public static AssistantMessage userMessage(String conversationId, String customerId, String content) {
        return userMessage(UUID.randomUUID().toString(), conversationId, customerId, content);
    }

    public static AssistantMessage userMessage(String id, String conversationId, String customerId, String content) {
        return createWithId(id, conversationId, customerId, "USER", "USER_INPUT", content, null);
    }

    public static AssistantMessage assistantReply(String conversationId, String customerId, String content, LocalDateTime createdAt) {
        return assistantReply(UUID.randomUUID().toString(), conversationId, customerId, content, createdAt);
    }

    public static AssistantMessage assistantReply(
            String id,
            String conversationId,
            String customerId,
            String content,
            LocalDateTime createdAt
    ) {
        var message = createWithId(id, conversationId, customerId, "ASSISTANT", "ASSISTANT_REPLY", content, null);
        message.createdAt = createdAt;
        message.readAt = createdAt;
        return message;
    }

    public static AssistantMessage priceIncrease(String conversationId, String customerId, String content, String sourceKey) {
        return create(conversationId, customerId, "ASSISTANT", "PRICE_INCREASE", content, sourceKey);
    }

    public static AssistantMessage financeNotice(String conversationId, String customerId, String content, String sourceKey) {
        return create(conversationId, customerId, "ASSISTANT", "FINANCE_NOTICE", content, sourceKey);
    }

    private static AssistantMessage create(
            String conversationId,
            String customerId,
            String role,
            String source,
            String content,
            String sourceKey
    ) {
        return createWithId(UUID.randomUUID().toString(), conversationId, customerId, role, source, content, sourceKey);
    }

    private static AssistantMessage createWithId(
            String id,
            String conversationId,
            String customerId,
            String role,
            String source,
            String content,
            String sourceKey
    ) {
        var message = new AssistantMessage();
        message.id = id;
        message.conversationId = conversationId;
        message.customerId = customerId;
        message.role = role;
        message.source = source;
        message.content = content;
        message.sourceKey = sourceKey;
        message.createdAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
        return message;
    }
}
