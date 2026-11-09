package com.example.demo.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.TestcontainersConfiguration;

/**
 * Simuliert die Render-Konfiguration: FRONTEND_URL ist gesetzt (hier absichtlich mit "/" am Ende).
 * Dann ist genau diese Adresse erlaubt, die localhost-Standardwerte gelten nicht mehr.
 */
@SpringBootTest(properties = "FRONTEND_URL=https://frontend.example.onrender.com/")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FrontendUrlCorsTest {

    @Autowired
    MockMvc mvc;

    @Test
    void allowsFrontendUrlEvenWithTrailingSlash() throws Exception {
        mvc.perform(options("/todos")
                        .header("Origin", "https://frontend.example.onrender.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://frontend.example.onrender.com"));
    }

    @Test
    void localhostIsNotAllowedWhenFrontendUrlIsSet() throws Exception {
        mvc.perform(options("/todos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
