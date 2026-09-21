package com.github.rony.estado.observability;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldGenerateRequestIdWhenHeaderIsAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        String generated = response.getHeader("X-Request-Id");
        assertThat(generated).isNotBlank();
    }

    @Test
    void shouldReuseRequestIdFromIncomingHeaderWhenItIsAValidUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String validUuid = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
        request.addHeader("X-Request-Id", validUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Request-Id")).isEqualTo(validUuid);
    }

    @Test
    void shouldExposeRequestIdViaMdcDuringChainExecutionAndClearItAfter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String validUuid = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
        request.addHeader("X-Request-Id", validUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcValueDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcValueDuringChain.set(MDC.get("requestId"));

        filter.doFilter(request, response, chain);

        assertThat(mdcValueDuringChain.get()).isEqualTo(validUuid);
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void shouldDiscardIncomingRequestIdWhenItIsNotAValidUuid() throws Exception {
        // Formato invalido (inclui tentativa de log injection via quebra de
        // linha) deve ser trocado por um UUID gerado pela aplicacao, nunca
        // refletido como veio - protege header de resposta, MDC/log e o
        // corpo de erro (GlobalExceptionHandler) de valores arbitrarios do
        // cliente.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "abc\n2026-09-21 ERROR forged log line");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        String generated = response.getHeader("X-Request-Id");
        assertThat(generated)
                .doesNotContain("\n")
                .doesNotContain("forged")
                .matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }
}
