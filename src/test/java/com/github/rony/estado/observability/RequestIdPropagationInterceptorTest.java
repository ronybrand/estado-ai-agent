package com.github.rony.estado.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestIdPropagationInterceptorTest {

    private final RequestIdPropagationInterceptor interceptor = new RequestIdPropagationInterceptor();

    @AfterEach
    void clearMdc() {
        MDC.remove(CorrelationIdFilter.MDC_KEY);
    }

    @Test
    void shouldForwardRequestIdFromMdcAsHeader() throws IOException {
        MDC.put(CorrelationIdFilter.MDC_KEY, "8f14e45f-ceea-4b78-8b0f-000000000001");

        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(null);

        interceptor.intercept(request, new byte[0], execution);

        assertThat(headers.getFirst("X-Request-Id")).isEqualTo("8f14e45f-ceea-4b78-8b0f-000000000001");
        verify(execution).execute(request, new byte[0]);
    }

    @Test
    void shouldNotSetHeaderWhenMdcHasNoRequestId() throws IOException {
        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(null);

        interceptor.intercept(request, new byte[0], execution);

        assertThat(headers.getFirst("X-Request-Id")).isNull();
    }
}
