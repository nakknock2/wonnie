package com.woori.wonnie.assistant;

import com.woori.wonnie.api.ApiDtos.AssistantMessageExchange;
import com.woori.wonnie.api.ApiDtos.AssistantMessageResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AssistantStreamEventTests {
    private static final JsonMapper JSON = JsonMapper.builder().findAndAddModules().build();
    private static final UUID REQUEST = UUID.fromString("20000000-0000-4000-8000-000000000001");

    @Test void allEventTypesSerializeExactlyAsTheSharedFrontendExamples() throws Exception {
        var examples = JSON.readTree(Files.readString(Path.of("../../docs/examples/assistant-events-v1.json")));
        var user = message("user-1", "USER", "USER_INPUT", "안녕");
        var assistant = message("assistant-1", "ASSISTANT", "ASSISTANT_REPLY", "안녕하세요 🌱");
        List<AssistantStreamEvent.Data> data = List.of(
                new AssistantStreamEvent.Connected("connection-1", 1),
                new AssistantStreamEvent.MessageCreated(message("notice-1", "ASSISTANT", "PRICE_INCREASE", "구독료가 변경됐어요.")),
                new AssistantStreamEvent.UnreadCountChanged(0),
                new AssistantStreamEvent.ReplyStarted("user-1"),
                new AssistantStreamEvent.ReplyDelta(1, "안녕하세요 🌱\n "),
                new AssistantStreamEvent.ReplyCompleted(new AssistantMessageExchange(user, assistant), 0),
                new AssistantStreamEvent.ReplyFailed("ASSISTANT_UNAVAILABLE", "답변을 생성하지 못했어요. 다시 시도해 주세요.", true),
                new AssistantStreamEvent.ReplyCancelled("USER_REQUESTED"));
        assertThat(examples.size()).isEqualTo(data.size()).isEqualTo(AssistantStreamEvent.Type.values().length);
        for (int i = 0; i < data.size(); i++) {
            var example = examples.get(i);
            var event = new AssistantStreamEvent(1, UUID.fromString(example.get("eventId").asString()), data.get(i).type(),
                    Instant.parse(example.get("occurredAt").asString()), example.get("conversationId").asString(),
                    example.get("requestId").isNull() ? null : UUID.fromString(example.get("requestId").asString()),
                    example.get("messageId").isNull() ? null : example.get("messageId").asString(), data.get(i));
            assertThat(JSON.readTree(JSON.writeValueAsString(event))).isEqualTo(example);
        }
    }

    @Test void rejectsMissingRequestIdentityAndMismatchedPayloadOrMessageIdentity() {
        var delta = new AssistantStreamEvent.ReplyDelta(1, " ");
        assertThatThrownBy(() -> AssistantStreamEvent.create("conversation", null, "assistant", delta))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AssistantStreamEvent.create("conversation", REQUEST, null, delta))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AssistantStreamEvent(1, UUID.randomUUID(), AssistantStreamEvent.Type.CONNECTED,
                Instant.now(), "conversation", REQUEST, "assistant", delta)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AssistantStreamEvent.create("conversation", null, "wrong-id",
                new AssistantStreamEvent.MessageCreated(message("stored-id", "ASSISTANT", "PRICE_INCREASE", "알림"))))
                .isInstanceOf(IllegalArgumentException.class);
        var exchange = new AssistantMessageExchange(message("user", "USER", "USER_INPUT", "질문"),
                message("stored-assistant", "ASSISTANT", "ASSISTANT_REPLY", "답변"));
        assertThatThrownBy(() -> AssistantStreamEvent.create("conversation", REQUEST, "wrong-assistant",
                new AssistantStreamEvent.ReplyCompleted(exchange, 0))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void preservesWhitespaceDeltasAndRejectsEmptyChunksAndInvalidCounters() {
        assertThat(new AssistantStreamEvent.ReplyDelta(1, "\n ").delta()).isEqualTo("\n ");
        assertThatThrownBy(() -> new AssistantStreamEvent.ReplyDelta(0, "text")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AssistantStreamEvent.ReplyDelta(1, "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AssistantStreamEvent.UnreadCountChanged(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    private static AssistantMessageResponse message(String id, String role, String source, String content) {
        return new AssistantMessageResponse(id, role, source, content, LocalDateTime.of(2026, 10, 27, 17, 0));
    }
}
