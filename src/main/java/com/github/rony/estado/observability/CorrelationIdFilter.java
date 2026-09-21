package com.github.rony.estado.observability;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

// Gera/propaga um id de correlacao por requisicao (X-Request-Id), disponivel
// no MDC durante toda a cadeia de filtros/controller para aparecer em todo
// log da requisicao, e devolvido no header de resposta para o cliente
// conseguir correlacionar com o proprio log dele.
@Component
@Order(-2)
public class CorrelationIdFilter implements Filter {

    static final String HEADER = "X-Request-Id";
    // public: unica fonte de verdade da chave MDC, tambem lida pelo
    // GlobalExceptionHandler (pacote exception) pra ecoar o requestId no
    // corpo de erro.
    public static final String MDC_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestId = httpRequest.getHeader(HEADER);
        if (!isValidUuid(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        httpResponse.setHeader(HEADER, requestId);
        MDC.put(MDC_KEY, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    // O valor recebido do cliente vai parar no MDC (logado em toda linha da
    // requisicao), no header de resposta e no corpo JSON de erro - sem essa
    // validacao, um cliente poderia injetar quebras de linha (log forging)
    // ou uma string enorme nesses tres lugares. So aceita o formato que a
    // aplicacao mesmo gera (UUID); qualquer coisa fora disso e substituida
    // por um UUID novo em vez de tentar sanitizar.
    private static boolean isValidUuid(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
