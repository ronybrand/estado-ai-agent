package com.github.rony.estado.config;

import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatClientConfigTest {

    @Test
    void geminiHttpOptionsConfiguraOTimeoutInformadoEmMilissegundos() {
        HttpOptions httpOptions = ChatClientConfig.geminiHttpOptions(20_000);

        assertThat(httpOptions.timeout()).contains(20_000);
    }

    @Test
    void geminiHttpOptionsDesligaORetryInternoDoSdk() {
        // O SDK repete a chamada com espera exponencial (5 tentativas por padrao),
        // o que transformou um timeout de 15 s em ~55 s antes de o AskService
        // poder tentar o modelo reserva.
        HttpOptions httpOptions = ChatClientConfig.geminiHttpOptions(15_000);

        assertThat(httpOptions.retryOptions().flatMap(HttpRetryOptions::attempts)).contains(1);
    }
}
