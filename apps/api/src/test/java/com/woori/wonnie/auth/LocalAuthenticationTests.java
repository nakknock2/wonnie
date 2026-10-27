package com.woori.wonnie.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalAuthenticationTests {
    @Autowired MockMvc http;

    @Test
    void localRequestsAreAuthenticatedAsTheDemoCustomer() throws Exception {
        http.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cust-kimwoori"))
                .andExpect(jsonPath("$.name").value("김우리"))
                .andExpect(jsonPath("$.email").value(""))
                .andExpect(jsonPath("$.picture").doesNotExist());
    }
}
