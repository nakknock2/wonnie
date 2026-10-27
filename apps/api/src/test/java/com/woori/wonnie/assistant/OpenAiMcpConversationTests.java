package com.woori.wonnie.assistant;

import com.sun.net.httpserver.HttpServer;
import com.woori.wonnie.finance.FinanceService;
import org.junit.jupiter.api.*;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.ai.openai.api-key=test-key-not-real","spring.ai.openai.timeout=5s",
    "spring.datasource.url=jdbc:h2:mem:wonnie-ai-tests;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"})
@ActiveProfiles({"local","openai"})
@AutoConfigureMockMvc
@Transactional
class OpenAiMcpConversationTests {
    static final List<String> REQUESTS=new CopyOnWriteArrayList<>();
    static final AtomicBoolean FAIL=new AtomicBoolean();
    static final AtomicBoolean LOOP=new AtomicBoolean();
    static final AtomicBoolean FAIL_EMBEDDING=new AtomicBoolean();
    static final List<String> EMBEDDINGS=new CopyOnWriteArrayList<>();
    static final JsonMapper JSON=JsonMapper.builder().findAndAddModules().build();
    static final HttpServer OPENAI=startServer();
    @MockitoBean WonnieMcpTools mcp;
    @Autowired FinanceService finance;
    @Autowired AssistantMessageRepository messages;
    @Autowired MockMvc http;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url",()->"http://127.0.0.1:"+OPENAI.getAddress().getPort());
    }
    @BeforeEach void resetStub(){
        REQUESTS.clear();EMBEDDINGS.clear();FAIL.set(false);LOOP.set(false);FAIL_EMBEDDING.set(false);
        when(mcp.callbacks()).thenReturn(new ToolCallback[]{new ToolCallback(){
            public ToolDefinition getToolDefinition(){return ToolDefinition.builder().name("get_subscriptions")
                .description("Read subscriptions").inputSchema("{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}").build();}
            public String call(String input){return JSON.writeValueAsString(finance.report("subscriptions",null));}
        }});
    }
    @AfterAll static void closeServer(){OPENAI.stop(0);}
    @Test void executesToolLoopAndPersistsConversationWithRecentHistory() throws Exception {
        http.perform(post("/api/assistant/messages").contentType("application/json").content("{\"message\":\"구독 총액 알려줘\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.assistantMessage.content").value("구독 총액을 조회했어요."));
        assertThat(REQUESTS).hasSize(2);
        assertThat(REQUESTS.get(1)).contains("DATABASE","projectedMonthlyCostAfterTrials","tool_call_id");
        assertThat(REQUESTS.get(0)).doesNotContain("dataSource=DEMO이면 샘플 데이터임을 답변에 밝힌다");
        assertThat(messages.count()).isEqualTo(2);
        REQUESTS.clear();
        http.perform(post("/api/assistant/messages").contentType("application/json").content("{\"message\":\"그 내용을 다시 설명해줘\"}"))
            .andExpect(status().isOk());
        assertThat(REQUESTS.get(0)).contains("구독 총액 알려줘","구독 총액을 조회했어요.","그 내용을 다시 설명해줘");
        assertThat(messages.count()).isEqualTo(4);
    }
    @Test void selectedFrontendTopicReachesModelWithoutChangingStoredUserText() throws Exception {
        http.perform(post("/api/assistant/messages").contentType("application/json")
                .content("{\"message\":\"조건 알려줘\",\"topic\":\"구독 카드 혜택 비교\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.userMessage.content").value("조건 알려줘"));
        assertThat(REQUESTS.get(0)).contains("구독 카드 혜택 비교", "조건 알려줘");
        assertThat(messages.findAll()).filteredOn(m -> "USER".equals(m.getRole()))
            .allSatisfy(m -> assertThat(m.getContent()).isEqualTo("조건 알려줘"));
    }
    @Test void rejectsOversizedFrontendTopicBeforeCallingModel() throws Exception {
        var body=JSON.writeValueAsString(java.util.Map.of("message","조건 알려줘","topic","a".repeat(201)));
        http.perform(post("/api/assistant/messages").contentType("application/json").content(body))
            .andExpect(status().isBadRequest());
        assertThat(REQUESTS).isEmpty();
    }
    @Test void upstreamFailureReturns503WithoutSavingFakeAssistantReply() throws Exception {
        FAIL.set(true);
        var result = http.perform(post("/api/assistant/messages").contentType("application/json").content("{\"message\":\"구독 조회\"}"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("ASSISTANT_UNAVAILABLE"))
            .andReturn();
        assertThat(((AssistantUnavailableException) result.getResolvedException()).getStage()).isEqualTo("OPENAI_CHAT");
        assertThat(messages.count()).isZero();
    }
    @Test void mcpDiscoveryFailureIsIdentifiedBeforeCallingOpenAi() throws Exception {
        when(mcp.callbacks()).thenThrow(new IllegalStateException("MCP initialization failed"));
        var result = http.perform(post("/api/assistant/messages").contentType("application/json").content("{\"message\":\"구독 조회\"}"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("ASSISTANT_UNAVAILABLE"))
            .andReturn();
        assertThat(((AssistantUnavailableException) result.getResolvedException()).getStage()).isEqualTo("MCP_TOOLS");
        assertThat(REQUESTS).isEmpty();
        assertThat(messages.count()).isZero();
    }
    @Test void repeatedToolCallsStopAtRequestLimitWithoutSavingReply() throws Exception {
        LOOP.set(true);
        http.perform(post("/api/assistant/messages").contentType("application/json").content("{\"message\":\"계속 조회\"}"))
            .andExpect(status().isServiceUnavailable());
        assertThat(REQUESTS.size()).isLessThanOrEqualTo(14);
        assertThat(messages.count()).isZero();
    }
    @Test void sendsUpdatedRagGuidesAndRetrievedKnowledgeAlongsideMcpTools() throws Exception {
        http.perform(post("/api/assistant/messages").contentType("application/json")
                .content("{\"message\":\"넷플릭스 해지 방법 알려줘\"}"))
            .andExpect(status().isOk());
        assertThat(REQUESTS.get(0)).contains("R13", "RECEIVE_KKULMONEY", "05_examples.md",
                "kb:SVC-NETFLIX", "04_knowledge_base.md", "일반 텍스트 상담만 반환", "get_subscriptions");
        assertThat(EMBEDDINGS).isNotEmpty().allSatisfy(body -> assertThat(body).contains("text-embedding-3-small"));
    }
    @Test void embeddingFailureReturns503WithRagStageAndSavesNoReply() throws Exception {
        FAIL_EMBEDDING.set(true);
        var result=http.perform(post("/api/assistant/messages").contentType("application/json")
                .content("{\"message\":\"넷플릭스 해지 방법\"}"))
            .andExpect(status().isServiceUnavailable()).andReturn();
        assertThat(((AssistantUnavailableException)result.getResolvedException()).getStage()).isEqualTo("RAG_RETRIEVAL");
        assertThat(REQUESTS).isEmpty();
        assertThat(messages.count()).isZero();
    }
    private static HttpServer startServer(){
        try {
            var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/",exchange->{
                var request=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
                boolean embedding=exchange.getRequestURI().getPath().endsWith("/embeddings");
                (embedding?EMBEDDINGS:REQUESTS).add(request);
                boolean fail=embedding?FAIL_EMBEDDING.get():FAIL.get();
                int status=fail?401:200;
                String response;
                if(fail)response="{\"error\":{\"message\":\"test authentication failure\",\"type\":\"invalid_request_error\",\"code\":\"invalid_api_key\"}}";
                else if(embedding) {
                    var data=new java.util.ArrayList<java.util.Map<String,Object>>();
                    var input=JSON.readTree(request).get("input");
                    for(int i=0;i<input.size();i++) {
                        var text=input.get(i).asString();
                        boolean netflix=text.startsWith("## [")?text.startsWith("## [SVC-NETFLIX]"):text.contains("넷플릭스");
                        data.add(java.util.Map.of("object","embedding","index",i,"embedding",netflix?List.of(1.0,0.0,0.0):List.of(0.0,0.0,1.0)));
                    }
                    response=JSON.writeValueAsString(java.util.Map.of("object","list","model","text-embedding-3-small","data",data,
                            "usage",java.util.Map.of("prompt_tokens",1,"total_tokens",1)));
                }
                else if(LOOP.get() || !request.contains("tool_call_id"))response="""
                    {"id":"chatcmpl-test","object":"chat.completion","created":1,"model":"gpt-5.6-terra",
                     "choices":[{"index":0,"message":{"role":"assistant","content":null,"tool_calls":[{"id":"call-demo","type":"function","function":{"name":"get_subscriptions","arguments":"{}"}}]},"finish_reason":"tool_calls"}]}
                    """;
                else response="""
                    {"id":"chatcmpl-test","object":"chat.completion","created":1,"model":"gpt-5.6-terra",
                     "choices":[{"index":0,"message":{"role":"assistant","content":"구독 총액을 조회했어요."},"finish_reason":"stop"}]}
                    """;
                var bytes=response.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
                exchange.sendResponseHeaders(status,bytes.length);try(var out=exchange.getResponseBody()){out.write(bytes);}
            });server.start();return server;
        }catch(Exception e){throw new IllegalStateException(e);}
    }
}
