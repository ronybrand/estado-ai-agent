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
    void shouldReuseRequestIdFromIncomingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "client-generated-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("client-generated-id");
    }

    @Test
    void shouldExposeRequestIdViaMdcDuringChainExecutionAndClearItAfter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "traced-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcValueDuringChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcValueDuringChain.set(MDC.get("requestId"));

        filter.doFilter(request, response, chain);

        assertThat(mdcValueDuringChain.get()).isEqualTo("traced-id");
        assertThat(MDC.get("requestId")).isNull();
    }
}
