package com.woori.wonnie.mcp;

import com.sun.net.httpserver.HttpServer;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubscriptionToolsIntegrationTests {
    private static final List<String> REQUESTS=new CopyOnWriteArrayList<>();
    private static final AtomicInteger STATUS=new AtomicInteger(200);
    private static final HttpServer API=startApi();
    @LocalServerPort int port;
    private McpSyncClient client;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.api.base-url",()->"http://127.0.0.1:"+API.getAddress().getPort());
    }
    @BeforeEach void connect() {
        REQUESTS.clear();STATUS.set(200);
        client=McpClient.sync(HttpClientStreamableHttpTransport.builder("http://127.0.0.1:"+port).endpoint("/mcp").requestBuilder(java.net.http.HttpRequest.newBuilder().header("X-Wonnie-Customer-Id", "cust-test")).build())
            .requestTimeout(Duration.ofSeconds(10)).build();client.initialize();
    }
    @AfterEach void close(){client.closeGracefully();}
    @AfterAll static void stop(){API.stop(0);}
    @Test void discoversOnlyThirteenReadToolsWithoutCalendarOrFinancialWrites() {
        var tools=client.listTools().tools();
        assertThat(tools).hasSize(13).allSatisfy(t->{assertThat(t.annotations().readOnlyHint()).isTrue();assertThat(t.annotations().destructiveHint()).isFalse();});
        assertThat(tools).extracting(Tool::name).contains("get_subscriptions","explain_merchant","get_price_changes","recommend_cards","get_card_schedule")
            .doesNotContain("get_calendar_signals","create_payment_pause_plan");
    }
    @Test void eachFinanceToolCallsItsOwnApiEndpoint() {
        var tools=Map.ofEntries(Map.entry("get_subscriptions","subscriptions"),Map.entry("get_payment_readiness","payment-readiness"),
            Map.entry("get_price_changes","price-changes"),Map.entry("find_subscription_overlaps","overlaps"),
            Map.entry("get_renewal_alerts","renewals"),Map.entry("get_cancellation_summary","cancellations"),
            Map.entry("get_usage_analysis","usage"),Map.entry("recommend_cards","card-recommendations"),
            Map.entry("get_customer_context","customer-context"),Map.entry("get_card_schedule","card-schedule"),Map.entry("get_financial_alerts","alerts"));
        tools.forEach((tool,path)->{var result=call(tool,Map.of());assertThat(result.isError()).isFalse();assertThat(text(result)).contains("DATABASE",path);});
        assertThat(REQUESTS).hasSize(11).allMatch(r->r.startsWith("GET /api/internal/finance/"));
    }
    @Test void passesMerchantAsEncodedParameterAndRejectsMissingInput() {
        assertThat(call("explain_merchant",Map.of("merchantName","APPLE.COM/BILL & test")).isError()).isFalse();
        assertThat(REQUESTS.get(0)).contains("/api/internal/finance/merchant?name=APPLE.COM%2FBILL%20%26%20test");
        REQUESTS.clear();assertThat(call("explain_merchant",Map.of("merchantName"," ")).isError()).isTrue();assertThat(REQUESTS).isEmpty();
    }
    @Test void reportsUpstreamErrorWithoutPrivateResponseBody() {
        STATUS.set(500);var result=call("get_subscriptions",Map.of());
        assertThat(result.isError()).isTrue();assertThat(text(result)).contains("HTTP 500").doesNotContain("private-upstream-data");
    }
    @Test void retainsDatedPolicyLookupWithoutUsingUnknownBillingChannelAsDirect() {
        var result=call("get_payment_failure_policy",Map.of("serviceName","Netflix","billingChannel","UNKNOWN"));
        assertThat(text(result)).contains("미확인").doesNotContain("help.netflix.com");assertThat(REQUESTS).isEmpty();
    }
    private CallToolResult call(String name,Map<String,Object> args){return client.callTool(CallToolRequest.builder(name).arguments(args).build());}
    private String text(CallToolResult r){return r.content().stream().filter(TextContent.class::isInstance).map(TextContent.class::cast).map(TextContent::text).reduce("",String::concat);}
    private static HttpServer startApi() {
        try {
            var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/",exchange->{
                REQUESTS.add(exchange.getRequestMethod()+" "+exchange.getRequestURI().toASCIIString());
                var body=STATUS.get()==200?"{\"dataSource\":\"DATABASE\",\"summary\":\""+exchange.getRequestURI().getPath()+"\",\"data\":{}}":"{\"secret\":\"private-upstream-data\"}";
                var bytes=body.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");
                exchange.sendResponseHeaders(STATUS.get(),bytes.length);try(var out=exchange.getResponseBody()){out.write(bytes);}
            });server.start();return server;
        }catch(Exception e){throw new IllegalStateException(e);}
    }
}
