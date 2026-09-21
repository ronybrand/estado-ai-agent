package com.github.rony.estado.ask;

import com.github.rony.estado.exception.ErrorCode;
import com.github.rony.estado.exception.GlobalExceptionHandler;
import com.github.rony.estado.exception.UpstreamServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AskController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AskControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AskService askService;

    @Test
    void shouldReturn400WhenQuestionIsBlank() throws Exception {
        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASK-01"));
    }

    @Test
    void shouldReturn400WhenQuestionExceedsMaxLength() throws Exception {
        String tooLong = "a".repeat(1001);

        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASK-01"));
    }

    @Test
    void shouldReturn200WithAnswerWhenQuestionIsValid() throws Exception {
        when(askService.ask(any())).thenReturn(new AskResponse("Mocked Answer"));

        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Qual a capital do Brasil?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Mocked Answer"));
    }

    @Test
    void shouldReturn502WhenUpstreamFails() throws Exception {
        when(askService.ask(any())).thenThrow(new UpstreamServiceException(
                ErrorCode.ASK_02_UPSTREAM_FAILURE, "Falha ao consultar a API de estados", new RuntimeException("boom")));

        mockMvc.perform(post("/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Qual a capital do Brasil?\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("ASK-02"));
    }
}
