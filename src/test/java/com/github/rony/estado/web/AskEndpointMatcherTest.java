package com.github.rony.estado.web;

import org.junit.jupiter.api.Test;
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

    @Test
    void shouldNotMatchPreflightOptionsToAsk() {
        // Preflight CORS (OPTIONS) nunca carrega os headers custom (X-API-Key)
        // nem deve ser contado no rate limit - e o navegador que gera essas
        // requisicoes automaticamente, nao o usuario.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");
        request.setRequestURI("/ask");

        assertThat(AskEndpointMatcher.appliesTo(request)).isFalse();
    }

    @Test
    void shouldNotMatchOtherRoutes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/actuator/health");

        assertThat(AskEndpointMatcher.appliesTo(request)).isFalse();
    }

    @Test
    void shouldNotMatchRoutesThatOnlyStartWithAskPrefix() {
        // "/ask" e um match exato, nao um prefixo: "/ask-admin" nao deve
        // ser afetado por engano.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/ask-admin");

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
