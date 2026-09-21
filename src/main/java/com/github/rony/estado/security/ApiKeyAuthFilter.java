package com.github.rony.estado.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.github.rony.estado.exception.ErrorCode;
import com.github.rony.estado.exception.ErrorResponseWriter;
import com.github.rony.estado.observability.CorrelationIdFilter;
import com.github.rony.estado.web.AskEndpointMatcher;
import com.github.rony.estado.web.FilterOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Component
@Order(FilterOrder.API_KEY_AUTH)
public class ApiKeyAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final String expectedApiKey;

    public ApiKeyAuthFilter(@Value("${app.security.api-key}") String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (AskEndpointMatcher.appliesTo(httpRequest)) {
            String providedKey = httpRequest.getHeader(API_KEY_HEADER);
            if (providedKey == null || !constantTimeEquals(providedKey, expectedApiKey)) {
                log.warn("Tentativa de acesso a /ask rejeitada (API key ausente ou invalida), ip={}",
                        httpRequest.getRemoteAddr());
                ErrorResponseWriter.write(httpResponse, HttpStatus.UNAUTHORIZED, ErrorCode.ASK_03_UNAUTHORIZED,
                        "API key ausente ou invalida", MDC.get(CorrelationIdFilter.MDC_KEY));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
