package com.woori.wonnie.api;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.woori.wonnie.assistant.AssistantUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTests {
    private final Logger logger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/assistant/messages");

    @BeforeEach void captureLogs() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach void releaseLogs() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test void logsProviderFailureWhileKeepingThePublicResponseGeneric() {
        var exception = new AssistantUnavailableException("OPENAI_CHAT",
                new IllegalStateException("OpenAI 404: model_not_found"));

        var response = handler.assistantUnavailable(exception, request);

        assertThat(appender.list).hasSize(1);
        var event = appender.list.get(0);
        assertThat(event.getLevel().toString()).isEqualTo("ERROR");
        assertThat(event.getFormattedMessage()).contains("ASSISTANT_UNAVAILABLE", "method=POST",
                "path=/api/assistant/messages", "stage=OPENAI_CHAT", "IllegalStateException", "model_not_found");
        assertThat(response.code()).isEqualTo("ASSISTANT_UNAVAILABLE");
        assertThat(response.message()).isEqualTo(exception.getMessage()).doesNotContain("model_not_found");
    }

    @Test void logsNestedMcpConnectionCauseWithoutChatOrAuthorizationHeaders() {
        request.setContent("private-chat-question".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Bearer request-secret");
        handler.assistantUnavailable(new AssistantUnavailableException("MCP_TOOLS",
                new IllegalStateException("MCP initialization failed", new java.net.ConnectException("Connection refused"))), request);

        assertThat(appender.list.get(0).getFormattedMessage()).contains("stage=MCP_TOOLS",
                "MCP initialization failed", "ConnectException", "Connection refused")
                .doesNotContain("private-chat-question", "request-secret");
    }

    @Test void redactsCredentialsAndDoesNotAttachAnUnfilteredThrowable() {
        handler.assistantUnavailable(new AssistantUnavailableException("OPENAI_CHAT",
                new IllegalStateException("invalid_api_key: sk-proj-secret-value api-key=other-secret "
                        + "Authorization: Bearer bearer-secret\nextra line")), request);

        var event = appender.list.get(0);
        assertThat(event.getFormattedMessage()).contains("invalid_api_key", "[REDACTED]")
                .doesNotContain("sk-proj-secret-value", "other-secret", "bearer-secret", "\n", "\r");
        assertThat(event.getThrowableProxy()).isNull();
    }
}
