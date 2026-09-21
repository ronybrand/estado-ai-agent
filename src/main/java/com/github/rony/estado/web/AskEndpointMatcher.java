package com.github.rony.estado.web;

import jakarta.servlet.http.HttpServletRequest;

// Fonte unica de verdade de "esta requisicao e um POST real para /ask" -
// usada por ApiKeyAuthFilter e RateLimitFilter, que antes duplicavam essa
// mesma logica (preflight CORS + match exato de rota) de forma identica,
// com risco de um dos dois divergir silenciosamente numa mudanca futura.
public final class AskEndpointMatcher {

    private static final String ASK_URI = "/ask";

    private AskEndpointMatcher() {
    }

    public static boolean appliesTo(HttpServletRequest request) {
        // Preflight CORS (OPTIONS) e gerado automaticamente pelo navegador
        // antes de um POST cross-origin, nunca carrega headers customizados
        // (X-API-Key) e nao deve ser contado no rate limit.
        boolean isPreflight = "OPTIONS".equalsIgnoreCase(request.getMethod());
        return !isPreflight && ASK_URI.equals(request.getRequestURI());
    }
}
