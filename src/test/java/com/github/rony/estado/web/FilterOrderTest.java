package com.github.rony.estado.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Trava a ordem relativa esperada da servlet filter chain contra regressao:
// sem isso, um novo filtro poderia ser adicionado com um numero de @Order
// que colide ou se intercala incorretamente com os existentes, sem que
// nada acuse o erro alem de um bug sutil em producao.
class FilterOrderTest {

    @Test
    void shouldRunCorrelationIdBeforeSecurityHeaders() {
        assertThat(FilterOrder.CORRELATION_ID).isLessThan(FilterOrder.SECURITY_HEADERS);
    }

    @Test
    void shouldRunSecurityHeadersBeforeRateLimit() {
        assertThat(FilterOrder.SECURITY_HEADERS).isLessThan(FilterOrder.RATE_LIMIT);
    }

    @Test
    void shouldRunRateLimitBeforeApiKeyAuth() {
        // Rate limit precisa rodar antes da autenticacao para que tentativas
        // de forca bruta da API key tambem sejam limitadas por IP.
        assertThat(FilterOrder.RATE_LIMIT).isLessThan(FilterOrder.API_KEY_AUTH);
    }
}
