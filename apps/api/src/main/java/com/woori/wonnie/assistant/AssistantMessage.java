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
@Table(name = "assistant_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssistantMessage {
    @Id
    private String id;

    @Column(nullable = false, length = 64)
    private String conversationId;

    @Column(name = "message_role", nullable = false, length = 20)
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

    public static AssistantMessage userMessage(String conversationId, String content) {
        return create(conversationId, "USER", "USER_INPUT", content, null);
    }

    public static AssistantMessage assistantReply(String conversationId, String content, LocalDateTime createdAt) {
        var message = create(conversationId, "ASSISTANT", "ASSISTANT_REPLY", content, null);
        message.createdAt = createdAt;
        message.readAt = createdAt;
        return message;
    }

    public static AssistantMessage priceIncrease(String conversationId, String content, String sourceKey) {
        return create(conversationId, "ASSISTANT", "PRICE_INCREASE", content, sourceKey);
    }

    public static AssistantMessage financeNotice(String conversationId, String content, String sourceKey) {
        return create(conversationId, "ASSISTANT", "FINANCE_NOTICE", content, sourceKey);
    }

    private static AssistantMessage create(
            String conversationId,
            String role,
            String source,
            String content,
            String sourceKey
    ) {
        var message = new AssistantMessage();
        message.id = UUID.randomUUID().toString();
        message.conversationId = conversationId;
        message.role = role;
        message.source = source;
        message.content = content;
        message.sourceKey = sourceKey;
        message.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        return message;
    }
}
