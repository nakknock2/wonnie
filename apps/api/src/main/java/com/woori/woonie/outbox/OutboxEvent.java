package com.woori.woonie.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    public static OutboxEvent pending(
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload
    ) {
        var event = new OutboxEvent();
        event.id = UUID.randomUUID().toString();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.payload = payload;
        event.status = "PENDING";
        event.createdAt = LocalDateTime.now();
        return event;
    }

    public void markPublished() {
        status = "PUBLISHED";
        publishedAt = LocalDateTime.now();
    }
}
