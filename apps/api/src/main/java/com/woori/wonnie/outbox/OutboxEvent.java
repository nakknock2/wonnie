package com.woori.wonnie.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "outbox_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    private String id;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String payload;
    @Column(name = "deduplication_key", unique = true, length = 180)
    private String deduplicationKey;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    public static OutboxEvent pending(
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            String deduplicationKey
    ) {
        var event = new OutboxEvent();
        event.id = UUID.randomUUID().toString();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.payload = payload;
        event.deduplicationKey = deduplicationKey;
        event.status = "PENDING";
        event.createdAt = LocalDateTime.now();
        return event;
    }

    public void markPublished() {
        status = "PUBLISHED";
        publishedAt = LocalDateTime.now();
    }
}
