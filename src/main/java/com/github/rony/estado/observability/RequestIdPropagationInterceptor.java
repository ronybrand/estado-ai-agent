package com.github.rony.estado.observability;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

// Repassa o X-Request-Id (MDC populado por CorrelationIdFilter) em toda
// chamada HTTP feita para a API de estados. Sem isso, o mesmo requestId
// aparecia so no log do ai-agent - a API de estados (que ja aceita e
// ecoa esse header, ver RequestIdFilter la) gerava o seu proprio, sem
// nenhuma forma de correlacionar as duas pontas da mesma requisicao do
// usuario nos logs.
public class RequestIdPropagationInterceptor implements ClientHttpRequestInterceptor {

    static final String HEADER = "X-Request-Id";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (requestId != null) {
            request.getHeaders().set(HEADER, requestId);
        }
        return execution.execute(request, body);
    }
}
