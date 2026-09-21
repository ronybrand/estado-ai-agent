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
        String[] origins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);

        // Falha no startup em vez de silenciosamente aceitar curinga: um "*"
        // em ASK_CORS_ALLOWED_ORIGINS (erro de configuracao) derrubaria a
        // unica camada que restringe quem chama /ask a partir do navegador -
        // a API key continuaria exigida, mas a defesa em profundidade do
        // CORS seria perdida sem nenhum aviso.
        for (String origin : origins) {
            if ("*".equals(origin)) {
                throw new IllegalStateException(
                        "ASK_CORS_ALLOWED_ORIGINS nao pode conter '*' - liste as origens explicitamente");
            }
        }

        return origins;
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
