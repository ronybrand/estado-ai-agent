package com.github.rony.estado.config;

import com.github.rony.estado.ask.EstadoTools;
import com.github.rony.estado.ask.SystemPrompt;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiConnectionProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    // Client proprio em vez do default da autoconfig do Spring AI (que
    // tambem produz um bean Client, mas e @ConditionalOnMissingBean - este
    // bean a substitui), so pra poder setar timeout. Sem isso, uma chamada
    // ao Gemini sem resposta fica presa indefinidamente: o rate limiter
    // (RateLimitFilter) so protege contra requisicoes novas, nao contra
    // as que ja estao em voo, entao algumas presas bastam pra esgotar o
    // pool de threads do servlet sob carga.
    @Bean
    public Client googleGenAiClient(
            GoogleGenAiConnectionProperties connectionProperties,
            @Value("${app.gemini.timeout-ms:20000}") int timeoutMs) {
        return Client.builder()
                .apiKey(connectionProperties.getApiKey())
                .httpOptions(geminiHttpOptions(timeoutMs))
                .build();
    }

    // Visivel pro teste (ChatClientConfigTest) sem precisar subir o Client
    // inteiro (que faria uma chamada real de rede pra validar a api key).
    static HttpOptions geminiHttpOptions(int timeoutMs) {
        return HttpOptions.builder().timeout(timeoutMs).build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, EstadoTools estadoTools) {
        return builder
                .defaultSystem(SystemPrompt.TEXT)
                .defaultTools(estadoTools)
                .build();
    }
}
