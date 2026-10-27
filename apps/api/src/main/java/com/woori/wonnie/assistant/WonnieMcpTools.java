package com.woori.wonnie.assistant;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import jakarta.annotation.PreDestroy;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
@Profile("openai")
public class WonnieMcpTools {
    public static final String CUSTOMER_HEADER = "X-Wonnie-Customer-Id";
    private static final Set<String> ALLOWED = Set.of(
            "get_subscriptions", "explain_merchant", "get_payment_readiness",
            "get_price_changes", "find_subscription_overlaps", "get_renewal_alerts",
            "get_cancellation_summary", "get_usage_analysis", "recommend_cards",
            "get_customer_context", "get_card_schedule", "get_financial_alerts",
            "get_payment_failure_policy", "get_subscription_checkout_options",
            "issue_woori_card", "create_subscription", "cancel_subscription");
    private final McpSyncClient client;
    private boolean initialized;

    public WonnieMcpTools(@Value("${app.mcp.base-url:http://127.0.0.1:8081}") String baseUrl) {
        var transport = HttpClientStreamableHttpTransport.builder(baseUrl)
                .endpoint("/mcp")
                .httpRequestCustomizer((request, method, uri, body, context) -> {
                    var customerId = AssistantCustomerContext.currentCustomerId();
                    if (customerId != null && !customerId.isBlank()) {
                        request.header(CUSTOMER_HEADER, customerId);
                    }
                })
                .build();
        client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
    }

    public synchronized ToolCallback[] callbacks() {
        try {
            if (!initialized) {
                client.initialize();
                initialized = true;
            }
            var tools = client.listTools().tools().stream()
                    .filter(tool -> ALLOWED.contains(tool.name()))
                    .filter(tool -> tool.annotations() != null)
                    .map(tool -> (ToolCallback) SyncMcpToolCallback.builder()
                            .mcpClient(client)
                            .tool(tool)
                            .prefixedToolName(tool.name())
                            .build())
                    .toArray(ToolCallback[]::new);
            if (tools.length == 0) {
                throw new IllegalStateException("No allowed MCP tools are available.");
            }
            return tools;
        } catch (RuntimeException exception) {
            initialized = false;
            throw exception;
        }
    }

    @PreDestroy
    public void close() {
        client.closeGracefully();
    }
}
