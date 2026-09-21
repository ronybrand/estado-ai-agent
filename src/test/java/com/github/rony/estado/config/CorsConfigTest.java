package com.github.rony.estado.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void shouldRejectWildcardOrigin() {
        // Erro de configuracao real que a validacao previne: ASK_CORS_ALLOWED_ORIGINS=*
        // derrubaria a unica camada que restringe quem chama /ask a partir do
        // navegador, sem nenhum aviso.
        assertThatThrownBy(() -> CorsConfig.parseOrigins("*"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("*");
    }

    @Test
    void shouldRejectWildcardOriginMixedWithValidOnes() {
        assertThatThrownBy(() -> CorsConfig.parseOrigins("http://localhost:4200,*"))
                .isInstanceOf(IllegalStateException.class);
    }
}
