package com.github.rony.estado.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AskEndpointMatcherTest {

    @Test
    void shouldMatchPostToAsk() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/ask");

        assertThat(AskEndpointMatcher.appliesTo(request)).isTrue();
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource({
            // Preflight CORS (OPTIONS) nunca carrega os headers custom (X-API-Key)
            // nem deve ser contado no rate limit - e o navegador que gera essas
            // requisicoes automaticamente, nao o usuario.
            "OPTIONS, /ask,       preflight OPTIONS para /ask",
            "POST,    /actuator/health, rota completamente diferente",
            // "/ask" e um match exato, nao um prefixo: "/ask-admin" nao deve
            // ser afetado por engano.
            "POST,    /ask-admin, rota que so comeca com o prefixo /ask",
    })
    void shouldNotMatch(String method, String uri, String scenario) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);

        assertThat(AskEndpointMatcher.appliesTo(request)).isFalse();
    }

    @Test
    void shouldMatchAskRegardlessOfMethodCaseSensitivity() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("post");
        request.setRequestURI("/ask");

        assertThat(AskEndpointMatcher.appliesTo(request)).isTrue();
    }
}
