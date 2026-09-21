package com.github.rony.estado.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void shouldTrimWhitespaceAroundOrigins() {
        // Erro humano comum ao preencher a env var: espaco depois da virgula
        // (ex.: "http://a, http://b"). Sem trim, o Spring compara a origem do
        // header Origin ("http://b") contra " http://b" (com espaco) e nunca
        // bate - o CORS falha silenciosamente so para essa origem.
        String[] origins = CorsConfig.parseOrigins("http://localhost:4200, https://d3bqbg07tehy1h.cloudfront.net");

        assertThat(origins).containsExactly("http://localhost:4200", "https://d3bqbg07tehy1h.cloudfront.net");
    }

    @Test
    void shouldParseSingleOriginWithoutComma() {
        String[] origins = CorsConfig.parseOrigins("http://localhost:4200");

        assertThat(origins).containsExactly("http://localhost:4200");
    }

    @Test
    void shouldIgnoreEmptyEntriesFromTrailingComma() {
        String[] origins = CorsConfig.parseOrigins("http://localhost:4200,");

        assertThat(origins).containsExactly("http://localhost:4200");
    }
}
