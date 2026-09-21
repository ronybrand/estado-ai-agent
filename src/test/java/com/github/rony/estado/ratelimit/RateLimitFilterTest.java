package com.github.rony.estado.ratelimit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    @Test
    void shouldAllowUnderLimit() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/ask");
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldBlockOverLimit() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter();
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/ask");
            request.setRemoteAddr("192.168.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
        }

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/ask");
        request.setRemoteAddr("192.168.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        verify(chain, times(10)).doFilter(any(), any());
    }

    @Test
    void shouldBypassPreflightOptionsRequestWithoutConsumingBucket() throws ServletException, IOException {
        // Preflight CORS (OPTIONS) e gerado automaticamente pelo navegador,
        // nao pelo usuario - contar isso no rate limit faria usuarios
        // legitimos baterem no limite de 10 req/min bem antes do esperado.
        RateLimitFilter filter = new RateLimitFilter();
        FilterChain chain = mock(FilterChain.class);
        String ip = "10.10.10.10";

        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setMethod("OPTIONS");
            request.setRequestURI("/ask");
            request.setRemoteAddr(ip);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(200);
        }

        verify(chain, times(20)).doFilter(any(), any());
    }

    @Test
    void shouldNotShareRateLimitBucketWithRoutesThatOnlyStartWithAskPrefix() throws ServletException, IOException {
        // "/ask" e um match exato de rota, nao um prefixo: uma rota futura como "/ask-admin"
        // nao deve compartilhar o bucket de "/ask" nem ser limitada por engano.
        RateLimitFilter filter = new RateLimitFilter();
        FilterChain chain = mock(FilterChain.class);
        String ip = "172.16.0.1";

        // Esgota o limite de 10 req/min de "/ask" para este IP.
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/ask");
            request.setRemoteAddr(ip);
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }

        // Uma rota diferente que apenas comeca com "/ask" nao deve ser afetada.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/ask-admin");
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain, times(11)).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldLogWarnWithClientIpWhenRateLimitExceeded() throws ServletException, IOException {
        Logger logger = (Logger) LoggerFactory.getLogger(RateLimitFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            RateLimitFilter filter = new RateLimitFilter();
            FilterChain chain = mock(FilterChain.class);
            String ip = "203.0.113.5";

            for (int i = 0; i < 10; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.setRequestURI("/ask");
                request.setRemoteAddr(ip);
                filter.doFilter(request, new MockHttpServletResponse(), chain);
            }

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/ask");
            request.setRemoteAddr(ip);
            filter.doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(appender.list)
                    .anyMatch(event -> event.getLevel() == Level.WARN
                            && event.getFormattedMessage().contains(ip));
        } finally {
            logger.detachAppender(appender);
        }
    }
}
