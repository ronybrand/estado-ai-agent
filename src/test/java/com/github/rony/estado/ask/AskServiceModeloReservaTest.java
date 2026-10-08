package com.github.rony.estado.ask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.InterruptedIOException;

import com.google.genai.errors.ClientException;
import com.google.genai.errors.ServerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;

@ExtendWith(MockitoExtension.class)
class AskServiceModeloReservaTest {

    private static final String RESERVA = "modelo-reserva";
    private static final AskRequest PERGUNTA = new AskRequest("Qual a sigla de Santa Catarina?");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private static RuntimeException indisponivel() {
        return new RuntimeException("Failed to generate content",
                new ServerException(503, "UNAVAILABLE", "This model is currently experiencing high demand."));
    }

    private void principalFalhaCom(RuntimeException falha) {
        when(chatClient.prompt().user(anyString()).call().content()).thenThrow(falha);
    }

    private void reservaResponde(String resposta) {
        lenient().when(chatClient.prompt().user(anyString()).options(any(ChatOptions.Builder.class)).call().content())
                .thenReturn(resposta);
    }

    @Test
    void usaOModeloReservaQuandoOPrincipalEstaIndisponivel() {
        principalFalhaCom(indisponivel());
        reservaResponde("SC");

        AskResponse resposta = new AskService(chatClient, RESERVA).ask(PERGUNTA);

        assertThat(resposta.answer()).isEqualTo("SC");
    }

    @Test
    void pedeOModeloReservaPeloNomeConfigurado() {
        principalFalhaCom(indisponivel());
        ArgumentCaptor<ChatOptions.Builder> opcoes = ArgumentCaptor.forClass(ChatOptions.Builder.class);
        when(chatClient.prompt().user(anyString()).options(opcoes.capture()).call().content()).thenReturn("SC");

        new AskService(chatClient, RESERVA).ask(PERGUNTA);

        assertThat(opcoes.getValue().build().getModel()).isEqualTo(RESERVA);
    }

    @Test
    void usaOModeloReservaQuandoOPrincipalDaTimeout() {
        principalFalhaCom(new RuntimeException("Failed to generate content", new InterruptedIOException("timeout")));
        reservaResponde("SC");

        AskResponse resposta = new AskService(chatClient, RESERVA).ask(PERGUNTA);

        assertThat(resposta.answer()).isEqualTo("SC");
    }

    @Test
    void naoUsaOModeloReservaQuandoOErroNaoECapacidade() {
        principalFalhaCom(new RuntimeException("erro",
                new ClientException(400, "INVALID_ARGUMENT", "bad request")));
        reservaResponde("nao deveria chegar aqui");
        var servico = new AskService(chatClient, RESERVA);

        assertThatThrownBy(() -> servico.ask(PERGUNTA))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("erro");
    }

    @Test
    void naoTentaReservaQuandoNaoHaModeloReservaConfigurado() {
        principalFalhaCom(indisponivel());
        reservaResponde("nao deveria chegar aqui");
        var semReserva = new AskService(chatClient, "");

        assertThatThrownBy(() -> semReserva.ask(PERGUNTA)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void propagaAFalhaDoReservaGuardandoAPrincipalComoSuprimida() {
        principalFalhaCom(indisponivel());
        var falhaReserva = indisponivel();
        when(chatClient.prompt().user(anyString()).options(any(ChatOptions.Builder.class)).call().content())
                .thenThrow(falhaReserva);
        var servico = new AskService(chatClient, RESERVA);

        Throwable lancada = catchThrowable(() -> servico.ask(PERGUNTA));

        assertThat(lancada).isSameAs(falhaReserva);
        assertThat(lancada.getSuppressed()).hasSize(1);
    }
}
