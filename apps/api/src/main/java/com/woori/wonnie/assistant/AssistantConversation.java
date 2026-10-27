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
@Table(name = "assistant_conversation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssistantConversation {
    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String customerId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static AssistantConversation openFor(String customerId) {
        var now = LocalDateTime.now(java.time.ZoneOffset.UTC);
        var conversation = new AssistantConversation();
        conversation.id = UUID.randomUUID().toString();
        conversation.customerId = customerId;
        conversation.createdAt = now;
        conversation.updatedAt = now;
        return conversation;
    }

    public void touch() {
        updatedAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
    }
}
