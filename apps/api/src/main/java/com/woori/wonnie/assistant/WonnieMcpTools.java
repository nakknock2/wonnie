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
    private static final Set<String> ALLOWED=Set.of("get_subscriptions","explain_merchant","get_payment_readiness",
        "get_price_changes","find_subscription_overlaps","get_renewal_alerts","get_cancellation_summary",
        "get_usage_analysis","recommend_cards","get_customer_context","get_card_schedule","get_financial_alerts","get_payment_failure_policy");
    private final McpSyncClient client;
    private boolean initialized;
    public WonnieMcpTools(@Value("${app.mcp.base-url:http://127.0.0.1:8081}") String baseUrl) {
        var transport=HttpClientStreamableHttpTransport.builder(baseUrl).endpoint("/mcp").build();
        client=McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
    }
    public synchronized ToolCallback[] callbacks() {
        try {
            if(!initialized){client.initialize();initialized=true;}
            var tools=client.listTools().tools().stream().filter(t->ALLOWED.contains(t.name()))
                .filter(t->t.annotations()!=null && Boolean.TRUE.equals(t.annotations().readOnlyHint()))
                .map(t->(ToolCallback)SyncMcpToolCallback.builder().mcpClient(client).tool(t).prefixedToolName(t.name()).build())
                .toArray(ToolCallback[]::new);
            if(tools.length==0)throw new IllegalStateException("허용된 MCP 조회 도구가 없습니다.");
            return tools;
        } catch(RuntimeException exception){initialized=false;throw exception;}
    }
    @PreDestroy
    public void close(){client.closeGracefully();}
}
