package com.github.rony.estado.security;

import com.github.rony.estado.ratelimit.RateLimitFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// Garante que RateLimitFilter (@Order 1) roda antes de ApiKeyAuthFilter (@Order 2),
// para que tentativas de forca bruta da API key tambem sejam limitadas por IP,
// nao apenas requisicoes autenticadas com sucesso.
class SecurityFilterOrderTest {

    private static final String VALID_KEY = "correct-key";

    @Test
    void bruteForceAttemptsWithWrongApiKeyAreRateLimited() throws ServletException, IOException {
        RateLimitFilter rateLimitFilter = new RateLimitFilter(10, 1);
        ApiKeyAuthFilter apiKeyAuthFilter = new ApiKeyAuthFilter(VALID_KEY);
        FilterChain terminalChain = mock(FilterChain.class);

        // Encadeia rateLimitFilter -> apiKeyAuthFilter -> terminalChain, na mesma ordem
        // em que os @Order dos filtros os colocariam na servlet filter chain real.
        FilterChain chain = (req, res) -> apiKeyAuthFilter.doFilter(req, res, terminalChain);

        MockHttpServletResponse lastResponse = null;
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/ask");
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("X-API-Key", "wrong-key-" + i);
            lastResponse = new MockHttpServletResponse();
            rateLimitFilter.doFilter(request, lastResponse, chain);

            // Cada uma das 10 tentativas consome o bucket e falha na auth (401), nunca 429.
            assertThat(lastResponse.getStatus()).isEqualTo(401);
        }

        // A 11a tentativa, ainda com chave errada, deve ser barrada pelo rate limit (429)
        // antes mesmo de chegar ao ApiKeyAuthFilter.
        MockHttpServletRequest eleventhRequest = new MockHttpServletRequest();
        eleventhRequest.setRequestURI("/ask");
        eleventhRequest.setRemoteAddr("10.0.0.1");
        eleventhRequest.addHeader("X-API-Key", "wrong-key-final");
        MockHttpServletResponse eleventhResponse = new MockHttpServletResponse();

        rateLimitFilter.doFilter(eleventhRequest, eleventhResponse, chain);

        assertThat(eleventhResponse.getStatus()).isEqualTo(429);
        verify(terminalChain, never()).doFilter(any(), any());
    }
}
