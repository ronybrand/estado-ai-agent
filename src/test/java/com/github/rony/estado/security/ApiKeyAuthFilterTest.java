package com.github.rony.estado.security;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ApiKeyAuthFilterTest {

    private static final String VALID_KEY = "correct-key";

    @Test
    void shouldRejectMissingApiKey() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(VALID_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/ask");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectWrongApiKey() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(VALID_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/ask");
        request.addHeader("X-API-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldAllowCorrectApiKey() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(VALID_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/ask");
        request.addHeader("X-API-Key", VALID_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldBypassOtherRoutes() throws ServletException, IOException {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(VALID_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldBypassPreflightOptionsRequestEvenWithoutApiKey() throws ServletException, IOException {
        // Preflight CORS (OPTIONS) nunca carrega X-API-Key - navegadores nao
        // enviam headers customizados nele. Se esse filtro bloqueasse o
        // preflight com 401, o navegador nunca chegaria a mandar o POST real,
        // mesmo com CORS configurado corretamente no backend (bug real
        // encontrado testando a integracao com o Angular).
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(VALID_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");
        request.setRequestURI("/ask");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldBypassRoutesThatOnlyStartWithAskPrefix() throws ServletException, IOException {
        // "/ask" e um match exato de rota, nao um prefixo: uma rota futura como "/ask-admin"
        // ou "/askXPTO" nao deve ser protegida (nem afetada) por engano por este filtro.
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(VALID_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/ask-admin");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldLogWarnWithClientIpWhenApiKeyIsRejected() throws ServletException, IOException {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiKeyAuthFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            ApiKeyAuthFilter filter = new ApiKeyAuthFilter(VALID_KEY);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/ask");
            request.addHeader("X-API-Key", "wrong-key");
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertThat(appender.list)
                    .anyMatch(event -> event.getLevel() == Level.WARN
                            && event.getFormattedMessage().contains("10.0.0.1"));
        } finally {
            logger.detachAppender(appender);
        }
    }
}
