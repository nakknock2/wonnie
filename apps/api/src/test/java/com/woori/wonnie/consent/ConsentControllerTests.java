package com.woori.wonnie.consent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConsentControllerTests {
    @Autowired MockMvc http;

    @Test
    void persistsSubscriptionDataConsentAndRestoresIt() throws Exception {
        http.perform(get("/api/consents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionDataAgreed").value(false));

        http.perform(put("/api/consents/subscription-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionDataAgreed").value(true));

        http.perform(get("/api/consents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionDataAgreed").value(true));
    }

    @Test
    void persistsEveryNotificationChoiceIncludingAnAllOffDecision() throws Exception {
        http.perform(put("/api/consents/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"d1\":true,\"priceChange\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.decided").value(true))
                .andExpect(jsonPath("$.notifications.d1").value(true))
                .andExpect(jsonPath("$.notifications.priceChange").value(false));

        http.perform(put("/api/consents/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"d1\":false,\"priceChange\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.decided").value(true));

        http.perform(get("/api/consents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.decided").value(true))
                .andExpect(jsonPath("$.notifications.d1").value(false))
                .andExpect(jsonPath("$.notifications.priceChange").value(false));
    }
}
