package com.woori.wonnie.assistant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import com.woori.wonnie.api.ApiDtos.AssistantMessageExchange;
import com.woori.wonnie.api.ApiDtos.AssistantMessageResponse;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Version 1 wire contract. Transport and publication are implemented separately. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AssistantStreamEvent(
        int version,
        UUID eventId,
        Type type,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt,
        String conversationId,
        UUID requestId,
        String messageId,
        Data data
) {
    public AssistantStreamEvent {
        if (version != 1) throw new IllegalArgumentException("Unsupported assistant event version");
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(occurredAt, "occurredAt");
        requireId(conversationId, "conversationId");
        Objects.requireNonNull(data, "data");
        if (type != data.type()) throw new IllegalArgumentException("Event type does not match data");
        boolean reply = type.name().startsWith("REPLY_");
        if (reply) Objects.requireNonNull(requestId, "Reply events require requestId");
        if (reply || type == Type.MESSAGE_CREATED) requireId(messageId, "messageId");
        else if (requestId != null || messageId != null) {
            throw new IllegalArgumentException("Connection and unread events have no requestId or messageId");
        }
        if (data instanceof MessageCreated created && !created.message().id().equals(messageId)) {
            throw new IllegalArgumentException("messageId does not match stored message");
        }
        if (data instanceof ReplyCompleted completed
                && !completed.exchange().assistantMessage().id().equals(messageId)) {
            throw new IllegalArgumentException("messageId does not match completed assistant message");
        }
    }

    public static AssistantStreamEvent create(String conversationId, UUID requestId, String messageId, Data data) {
        Objects.requireNonNull(data, "data");
        return new AssistantStreamEvent(1, UUID.randomUUID(), data.type(), Instant.now(),
                conversationId, requestId, messageId, data);
    }

    public enum Type {
        CONNECTED("connected"),
        MESSAGE_CREATED("message.created"),
        UNREAD_COUNT_CHANGED("unread-count.changed"),
        REPLY_STARTED("reply.started"),
        REPLY_DELTA("reply.delta"),
        REPLY_COMPLETED("reply.completed"),
        REPLY_FAILED("reply.failed"),
        REPLY_CANCELLED("reply.cancelled");

        private final String wireName;
        Type(String wireName) { this.wireName = wireName; }
        @JsonValue public String wireName() { return wireName; }
    }

    public sealed interface Data permits Connected, MessageCreated, UnreadCountChanged,
            ReplyStarted, ReplyDelta, ReplyCompleted, ReplyFailed, ReplyCancelled {
        Type type();
    }

    public record Connected(String connectionId, long unreadCount) implements Data {
        public Connected { requireId(connectionId, "connectionId"); requireCount(unreadCount); }
        @Override public Type type() { return Type.CONNECTED; }
    }

    public record MessageCreated(AssistantMessageResponse message) implements Data {
        public MessageCreated { requireMessage(message); }
        @Override public Type type() { return Type.MESSAGE_CREATED; }
    }

    public record UnreadCountChanged(long unreadCount) implements Data {
        public UnreadCountChanged { requireCount(unreadCount); }
        @Override public Type type() { return Type.UNREAD_COUNT_CHANGED; }
    }

    public record ReplyStarted(String userMessageId) implements Data {
        public ReplyStarted { requireId(userMessageId, "userMessageId"); }
        @Override public Type type() { return Type.REPLY_STARTED; }
    }

    public record ReplyDelta(long sequence, String delta) implements Data {
        public ReplyDelta {
            if (sequence < 1) throw new IllegalArgumentException("sequence starts at 1");
            // Spaces and newlines are meaningful parts of a streamed answer.
            if (delta == null || delta.isEmpty()) throw new IllegalArgumentException("delta must not be empty");
        }
        @Override public Type type() { return Type.REPLY_DELTA; }
    }

    public record ReplyCompleted(AssistantMessageExchange exchange, long unreadCount) implements Data {
        public ReplyCompleted {
            Objects.requireNonNull(exchange, "exchange");
            requireMessage(exchange.userMessage());
            requireMessage(exchange.assistantMessage());
            if (!"USER".equals(exchange.userMessage().role()) || !"ASSISTANT".equals(exchange.assistantMessage().role())) {
                throw new IllegalArgumentException("Completed exchange requires USER and ASSISTANT messages");
            }
            requireCount(unreadCount);
        }
        @Override public Type type() { return Type.REPLY_COMPLETED; }
    }

    public record ReplyFailed(String code, String message, boolean retryable) implements Data {
        public ReplyFailed { requireText(code, "code"); requireText(message, "message"); }
        @Override public Type type() { return Type.REPLY_FAILED; }
    }

    public record ReplyCancelled(String reason) implements Data {
        public ReplyCancelled { requireText(reason, "reason"); }
        @Override public Type type() { return Type.REPLY_CANCELLED; }
    }

    private static void requireMessage(AssistantMessageResponse message) {
        Objects.requireNonNull(message, "message");
        requireId(message.id(), "message.id");
    }

    private static void requireId(String value, String name) {
        requireText(value, name);
        if (value.length() > 64) throw new IllegalArgumentException(name + " exceeds 64 characters");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }

    private static void requireCount(long count) {
        if (count < 0) throw new IllegalArgumentException("unreadCount must not be negative");
    }
}
