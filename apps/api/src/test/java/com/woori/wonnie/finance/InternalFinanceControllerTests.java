package com.woori.wonnie.finance;

import com.woori.wonnie.dashboard.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InternalFinanceControllerTests {
    @Autowired MockMvc http;

    @Test
    void internalFinanceEndpointRequiresServiceToken() throws Exception {
        http.perform(get("/api/internal/finance/subscriptions")
                        .header(InternalFinanceController.CUSTOMER_HEADER, DashboardService.DEMO_CUSTOMER_ID))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void internalFinanceEndpointUsesExplicitCustomerContext() throws Exception {
        http.perform(get("/api/internal/finance/subscriptions")
                        .header("X-Wonnie-Internal-Token", "local-development-token")
                        .header(InternalFinanceController.CUSTOMER_HEADER, DashboardService.DEMO_CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSource").value("DATABASE"))
                .andExpect(jsonPath("$.data.subscriptions").isArray());

        http.perform(get("/api/internal/finance/subscriptions")
                        .header("X-Wonnie-Internal-Token", "local-development-token")
                        .header(InternalFinanceController.CUSTOMER_HEADER, "cust-without-finance-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }
}
