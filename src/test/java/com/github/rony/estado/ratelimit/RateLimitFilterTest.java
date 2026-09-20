package com.github.rony.estado.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
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
        verify(chain, times(10)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
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

        verify(chain, times(11)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
