package com.github.rony.estado.ask;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;

import com.google.genai.errors.ClientException;
import com.google.genai.errors.ServerException;
import org.junit.jupiter.api.Test;

class GeminiFailuresTest {

    @Test
    void servicoIndisponivelPedeOutroModelo() {
        var falha = new RuntimeException("Failed to generate content",
                new ServerException(503, "UNAVAILABLE", "high demand"));

        assertThat(GeminiFailures.valeTentarOutroModelo(falha)).isTrue();
    }

    @Test
    void cotaExcedidaPedeOutroModelo() {
        var falha = new RuntimeException("Failed to generate content",
                new ClientException(429, "RESOURCE_EXHAUSTED", "quota"));

        assertThat(GeminiFailures.valeTentarOutroModelo(falha)).isTrue();
    }

    @Test
    void timeoutDaChamadaPedeOutroModelo() {
        var falha = new RuntimeException("Failed to generate content", new InterruptedIOException("timeout"));

        assertThat(GeminiFailures.valeTentarOutroModelo(falha)).isTrue();
    }

    @Test
    void timeoutDeSocketPedeOutroModelo() {
        var falha = new RuntimeException("erro", new RuntimeException(new SocketTimeoutException("read timed out")));

        assertThat(GeminiFailures.valeTentarOutroModelo(falha)).isTrue();
    }

    @Test
    void causaProfundaNaCadeiaEEncontrada() {
        var falha = new RuntimeException("a", new RuntimeException("b", new RuntimeException("c",
                new ServerException(503, "UNAVAILABLE", "high demand"))));

        assertThat(GeminiFailures.valeTentarOutroModelo(falha)).isTrue();
    }

    @Test
    void requisicaoInvalidaNaoPedeOutroModelo() {
        var falha = new RuntimeException("Failed to generate content",
                new ClientException(400, "INVALID_ARGUMENT", "bad request"));

        assertThat(GeminiFailures.valeTentarOutroModelo(falha)).isFalse();
    }

    @Test
    void chaveInvalidaNaoPedeOutroModelo() {
        var falha = new RuntimeException("erro", new ClientException(403, "PERMISSION_DENIED", "key"));

        assertThat(GeminiFailures.valeTentarOutroModelo(falha)).isFalse();
    }

    @Test
    void erroSemRelacaoNaoPedeOutroModelo() {
        assertThat(GeminiFailures.valeTentarOutroModelo(new IllegalStateException("bug"))).isFalse();
    }
}
