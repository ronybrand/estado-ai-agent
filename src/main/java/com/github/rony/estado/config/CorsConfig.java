package com.github.rony.estado.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    // Publico e estatico para ser testavel isoladamente (parsing puro, sem
    // precisar subir contexto Spring so pra validar como a string de env var
    // vira um array de origens).
    public static String[] parseOrigins(String allowedOrigins) {
        return java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer(
            @Value("${app.security.cors-allowed-origins}") String allowedOrigins) {
        String[] origins = parseOrigins(allowedOrigins);
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/ask")
                        .allowedOrigins(origins)
                        .allowedMethods("POST")
                        // X-Request-Id: o Angular anexa em toda requisicao via
                        // requestIdInterceptor, mesmo pro ai-agent (nao usado
                        // aqui hoje, mas precisa ser aceito no preflight senao
                        // o navegador bloqueia a chamada inteira por CORS).
                        .allowedHeaders("Content-Type", "X-API-Key", "X-Request-Id");
            }
        };
    }
}
