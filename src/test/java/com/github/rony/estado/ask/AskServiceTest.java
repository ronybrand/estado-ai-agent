package com.github.rony.estado.ask;

import com.github.rony.estado.exception.UpstreamServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AskServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @InjectMocks
    private AskService askService;

    @Test
    void shouldReturnAnswerFromChatClient() {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("Mocked Answer");

        AskResponse response = askService.ask(new AskRequest("Qual a capital do Brasil?"));

        assertThat(response.answer()).isEqualTo("Mocked Answer");
    }

    @Test
    void shouldWrapRestClientExceptionAsUpstreamServiceException() {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenThrow(new RestClientException("falha upstream"));

        AskRequest request = new AskRequest("Qual a capital do Brasil?");

        assertThatThrownBy(() -> askService.ask(request))
                .isInstanceOf(UpstreamServiceException.class)
                .hasMessageContaining("Falha ao consultar a API de estados");
    }
}
