package com.github.rony.estado.observability;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Actuator nao exigia nada alem de estar no classpath, mas sem
// management.endpoints.web.exposure.include configurado so /actuator/health
// fica exposto por padrao - este teste trava a exposicao esperada
// (health, info, metrics) contra regressao de configuracao.
@SpringBootTest(properties = {
        "spring.ai.google.genai.api-key=dummy-test-key",
        "estado.api.base-url=http://localhost:0",
        "app.security.api-key=test-api-key"
})
@AutoConfigureMockMvc
class ActuatorEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeHealthEndpointAsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldNotExposeHealthDetailsToUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void shouldExposeMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    void shouldNotRequireApiKeyForActuatorEndpoints() throws Exception {
        // /actuator/** fica fora do escopo do ApiKeyAuthFilter (que so
        // protege "/ask"); este teste documenta essa decisao explicitamente.
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
