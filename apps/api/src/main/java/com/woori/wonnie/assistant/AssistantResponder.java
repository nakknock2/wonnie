package com.woori.wonnie.assistant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public interface AssistantResponder {
    String respond(String customerId, String message, List<AssistantMessage> history);

    /**
     * Generates a reply while forwarding text chunks as they arrive. Implementations without
     * model streaming keep working by forwarding their completed response as one chunk.
     */
    default String respondStreaming(
            String customerId,
            String message,
            List<AssistantMessage> history,
            Consumer<String> onDelta
    ) {
        Objects.requireNonNull(onDelta, "onDelta");
        var answer = respond(customerId, message, history);
        onDelta.accept(answer);
        return answer;
    }
}
