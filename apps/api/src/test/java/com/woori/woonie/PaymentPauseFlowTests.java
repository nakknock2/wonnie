package com.woori.woonie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentPauseFlowTests {

    @Autowired
    MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsActionPlanAuthenticatesAndRunsSaga() throws Exception {
        var assistantResult = mockMvc.perform(post("/api/assistant/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"시험 기간 동안 넷플릭스 결제를 중지해줘\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionPlan.subscriptionName").value("Netflix"))
                .andReturn();

        JsonNode assistantJson = objectMapper.readTree(assistantResult.getResponse().getContentAsString());
        String actionPlanId = assistantJson.at("/actionPlan/id").asText();

        var challengeResult = mockMvc.perform(post("/api/action-plans/{id}/auth-challenge", actionPlanId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode challengeJson = objectMapper.readTree(challengeResult.getResponse().getContentAsString());

        mockMvc.perform(post("/api/action-plans/{id}/mock-face-id", actionPlanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(challengeJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.subscriptionStatus").value("PAYMENT_PAUSE_SCHEDULED"));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pausedCount").value(1));
    }
}
