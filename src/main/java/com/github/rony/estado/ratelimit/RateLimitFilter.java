package com.github.rony.estado.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // Roda antes do ApiKeyAuthFilter (Order 1 < 2) para que tentativas de força bruta
    // da API key também sejam limitadas por IP, não só requisições autenticadas.
    // Chave = IP do cliente (resolvido pelo ForwardedHeaderFilter a partir de X-Forwarded-For
    // quando server.forward-headers-strategy=framework está habilitado, evitando spoofing atrás de proxy).
    // Entradas expiram por inatividade para não crescer indefinidamente (evita OOM por IPs rotativos).
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    private Bucket createNewBucket() {
        // Limite de 10 requests por minuto por IP para o free tier do Gemini
        Bandwidth limit = Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Preflight CORS (OPTIONS) nao deve consumir o bucket do IP - e o
        // navegador que gera essas requisicoes automaticamente antes de um
        // POST cross-origin, nao o usuario; contar isso no rate limit faria
        // usuarios legitimos baterem no limite bem antes do esperado.
        boolean isPreflight = "OPTIONS".equalsIgnoreCase(httpRequest.getMethod());

        if (!isPreflight && "/ask".equals(httpRequest.getRequestURI())) {
            String ip = httpRequest.getRemoteAddr();
            Bucket bucket = buckets.get(ip, key -> createNewBucket());

            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
                log.warn("Rate limit excedido para /ask, ip={}", ip);
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResponse.getWriter().write("Too many requests");
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
