package com.github.rony.estado.config;

import com.google.genai.types.HttpOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatClientConfigTest {

    @Test
    void geminiHttpOptionsConfiguraOTimeoutInformadoEmMilissegundos() {
        HttpOptions httpOptions = ChatClientConfig.geminiHttpOptions(20_000);

        assertThat(httpOptions.timeout()).contains(20_000);
    }
}
