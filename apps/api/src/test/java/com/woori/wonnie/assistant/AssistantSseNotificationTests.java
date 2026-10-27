package com.woori.wonnie.assistant;

import com.woori.wonnie.dashboard.DashboardService;
import com.woori.wonnie.finance.BusinessClock;
import com.woori.wonnie.consent.ConsentService;
import com.woori.wonnie.subscription.SubscriptionPriceObservationEvent;
import com.woori.wonnie.subscription.SubscriptionPriceObservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.shutdown=immediate",
        "spring.datasource.url=jdbc:h2:mem:wonnie-sse-tests;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
class AssistantSseNotificationTests {
    private static final JsonMapper JSON = JsonMapper.builder().findAndAddModules().build();

    @LocalServerPort int port;
    @Autowired AssistantService assistantService;
    @Autowired AssistantMessageRepository messageRepository;
    @Autowired SubscriptionPriceObservationService observationService;
    @Autowired BusinessClock clock;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ConsentService consents;

    @Test
    void emitsCommittedNoticeAndUnreadEventsButNotRolledBackNotice() throws Exception {
        var customerId = DashboardService.DEMO_CUSTOMER_ID;
        consents.updateNotifications(customerId, false, true);
        var unreadBefore = assistantService.unreadCount(customerId);
        var connection = (HttpURLConnection) URI.create("http://127.0.0.1:" + port + "/api/assistant/events")
                .toURL().openConnection();
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setReadTimeout(5_000);
        connection.connect();

        var reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        var executor = Executors.newSingleThreadExecutor(task -> {
            var thread = new Thread(task, "assistant-sse-test-reader");
            thread.setDaemon(true);
            return thread;
        });
        try {
            assertThat(connection.getResponseCode()).isEqualTo(200);
            assertThat(connection.getContentType()).startsWith("text/event-stream");

            var connected = readFrame(reader);
            var connectedEnvelope = envelope(connected);
            assertThat(connected.get("event")).isEqualTo("connected");
            assertThat(connected.get("id")).isEqualTo(connectedEnvelope.get("eventId").asString());
            assertThat(connectedEnvelope.get("data").get("unreadCount").asLong()).isEqualTo(unreadBefore);

            var nextFrameStarted = new CountDownLatch(1);
            var nextFrame = executor.submit(() -> {
                nextFrameStarted.countDown();
                return readFrame(reader);
            });
            assertThat(nextFrameStarted.await(1, TimeUnit.SECONDS)).isTrue();

            var rollbackKey = "sse-rollback:" + UUID.randomUUID();
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                assistantService.addFinanceNotice(customerId, "롤백되면 전달되지 않을 알림", rollbackKey);
                assertThatThrownBy(() -> nextFrame.get(200, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
                status.setRollbackOnly();
            });
            assertThat(messageRepository.existsBySourceKey(rollbackKey)).isFalse();

            var observedAt = clock.nowUtc().plusSeconds(1);
            observationService.process(priceEvent("sse-first-" + UUID.randomUUID(), "2200", "2.99", observedAt));
            observationService.process(priceEvent("sse-fx-" + UUID.randomUUID(), "4600", "2.99", observedAt.plusSeconds(1)));

            var fxNotice = nextFrame.get(3, TimeUnit.SECONDS);
            var fxEnvelope = envelope(fxNotice);
            assertThat(fxNotice.get("event")).isEqualTo("message.created");
            assertThat(fxEnvelope.get("data").get("message").get("source").asString())
                    .isEqualTo("FINANCE_NOTICE");
            assertThat(fxEnvelope.get("data").get("message").get("content").asString())
                    .contains("환율·수수료");

            var fxUnread = readFrame(reader);
            assertThat(fxUnread.get("event")).isEqualTo("unread-count.changed");
            assertThat(envelope(fxUnread).get("data").get("unreadCount").asLong()).isEqualTo(unreadBefore + 1);

            observationService.process(priceEvent("sse-price-" + UUID.randomUUID(), "6000", "3.99", observedAt.plusSeconds(2)));
            var messageCreated = readFrame(reader);
            var messageEnvelope = envelope(messageCreated);
            assertThat(messageCreated.get("event")).isEqualTo("message.created");
            assertThat(messageCreated.get("id")).isEqualTo(messageEnvelope.get("eventId").asString());
            assertThat(messageEnvelope.get("data").get("message").get("source").asString())
                    .isEqualTo("PRICE_INCREASE");
            assertThat(messageEnvelope.get("data").get("message").get("content").asString()).contains("3.99 USD");
            assertThat(messageEnvelope.get("messageId").asString())
                    .isEqualTo(messageEnvelope.get("data").get("message").get("id").asString());

            var unreadChanged = readFrame(reader);
            var unreadEnvelope = envelope(unreadChanged);
            assertThat(unreadChanged.get("event")).isEqualTo("unread-count.changed");
            assertThat(unreadEnvelope.get("data").get("unreadCount").asLong()).isEqualTo(unreadBefore + 2);

            assistantService.markRead(customerId);
            var readChanged = readFrame(reader);
            assertThat(readChanged.get("event")).isEqualTo("unread-count.changed");
            assertThat(envelope(readChanged).get("data").get("unreadCount").asLong()).isZero();
        } finally {
            reader.close();
            connection.disconnect();
            executor.shutdownNow();
        }
    }

    private static Map<String, String> readFrame(BufferedReader reader) throws Exception {
        var fields = new HashMap<String, String>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) return fields;
            if (line.startsWith(":")) continue;
            var separator = line.indexOf(':');
            if (separator < 0) continue;
            var value = line.substring(separator + 1);
            if (value.startsWith(" ")) value = value.substring(1);
            fields.put(line.substring(0, separator), value);
        }
        throw new IllegalStateException("SSE stream ended before an event frame arrived.");
    }

    private static tools.jackson.databind.JsonNode envelope(Map<String, String> frame) throws Exception {
        return JSON.readTree(frame.get("data"));
    }

    private static SubscriptionPriceObservationEvent priceEvent(
            String eventId, String amount, String originalAmount, LocalDateTime observedAt) {
        var amountValue = new BigDecimal(amount);
        var originalValue = new BigDecimal(originalAmount);
        return new SubscriptionPriceObservationEvent(eventId, DashboardService.DEMO_CUSTOMER_ID,
                "sub-apple", amountValue, observedAt, "USD", originalValue,
                amountValue.divide(originalValue, 6, java.math.RoundingMode.HALF_UP));
    }
}
