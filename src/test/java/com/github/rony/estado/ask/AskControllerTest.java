package com.github.rony.estado.ask;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AskControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @InjectMocks
    private AskController askController;

    @Test
    void shouldReturnAskResponse() {
        // Mock the fluent ChatClient chain using deep stubs
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("Mocked Answer");
        AskRequest request = new AskRequest("Qual a capital do Brasil?");
        AskResponse response = askController.ask(request);
        assertEquals("Mocked Answer", response.answer());
    }
}
