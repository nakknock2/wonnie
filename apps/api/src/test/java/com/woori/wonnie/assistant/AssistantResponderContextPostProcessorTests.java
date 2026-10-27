package com.woori.wonnie.assistant;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantResponderContextPostProcessorTests {
    @Test
    void exposesCustomerContextForStreamingRepliesAndClearsItAfterward() {
        var seenCustomer = new AtomicReference<String>();
        AssistantResponder target = new AssistantResponder() {
            @Override
            public String respond(String customerId, String message, List<AssistantMessage> history) {
                return "unused";
            }

            @Override
            public String respondStreaming(String customerId, String message,
                    List<AssistantMessage> history, java.util.function.Consumer<String> onDelta) {
                seenCustomer.set(AssistantCustomerContext.currentCustomerId());
                onDelta.accept("streamed");
                return "streamed";
            }
        };

        var processed = new AssistantResponderContextPostProcessor()
                .postProcessAfterInitialization(target, "assistantResponder");
        var responder = (AssistantResponder) processed;
        var delta = new AtomicReference<String>();

        assertThat(responder.respondStreaming("cust-stream", "question", List.of(), delta::set))
                .isEqualTo("streamed");
        assertThat(delta).hasValue("streamed");
        assertThat(seenCustomer).hasValue("cust-stream");
        assertThat(AssistantCustomerContext.currentCustomerId()).isNull();
    }
}
