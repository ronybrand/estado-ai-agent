package com.github.rony.estado.ask;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Teste fim-a-fim: sobe o contexto Spring inteiro (filtros de seguranca,
// rate limit, correlation id, CORS, controller e exception handler reais)
// via @AutoConfigureMockMvc com o webAppContext completo (addFilters=true,
// o padrao), ao contrario de AskControllerWebMvcTest (@WebMvcTest com
// addFilters=false). Apenas AskService e mockado, para nao depender do
// Gemini/API de estados de verdade - todo o resto roda como em producao.
@SpringBootTest(properties = {
        "spring.ai.google.genai.api-key=dummy-test-key",
        "estado.api.base-url=http://localhost:0",
        "app.security.api-key=test-api-key",
        "app.security.cors-allowed-origins=http://localhost:4200"
})
@AutoConfigureMockMvc
class AskEndToEndIntegrationTest {

    private static final String VALID_API_KEY = "test-api-key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AskService askService;

    @Test
    void shouldAnswerQuestionThroughFullStackWithValidApiKey() throws Exception {
        when(askService.ask(any())).thenReturn(new AskResponse("Brasilia"));

        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", VALID_API_KEY)
                        .header("X-Forwarded-For", "203.0.113.10")
                        .content("{\"question\":\"Qual a capital do Brasil?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Brasilia"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void shouldRejectRequestWithoutApiKeyThroughRealFilterChain() throws Exception {
        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.11")
                        .content("{\"question\":\"Qual a capital do Brasil?\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidQuestionWithStandardErrorBody() throws Exception {
        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", VALID_API_KEY)
                        .header("X-Forwarded-For", "203.0.113.12")
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASK-01"));
    }

    @Test
    void shouldReturn429WhenRateLimitExceededThroughRealFilterChain() throws Exception {
        when(askService.ask(any())).thenReturn(new AskResponse("Brasilia"));

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", VALID_API_KEY)
                    .header("X-Forwarded-For", "203.0.113.13")
                    .content("{\"question\":\"Qual a capital do Brasil?\"}"));
        }

        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", VALID_API_KEY)
                        .header("X-Forwarded-For", "203.0.113.13")
                        .content("{\"question\":\"Qual a capital do Brasil?\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldEchoClientProvidedCorrelationIdEndToEnd() throws Exception {
        when(askService.ask(any())).thenReturn(new AskResponse("Brasilia"));
        String requestId = "8f14e45f-ceea-4b78-8b0f-000000000001";

        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", VALID_API_KEY)
                        .header("X-Forwarded-For", "203.0.113.14")
                        .header("X-Request-Id", requestId)
                        .content("{\"question\":\"Qual a capital do Brasil?\"}"))
                .andExpect(header().string("X-Request-Id", requestId));
    }

    @Test
    void shouldNotAllowCrossOriginRequestFromUnlistedOriginThroughRealCorsConfig() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/ask")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
