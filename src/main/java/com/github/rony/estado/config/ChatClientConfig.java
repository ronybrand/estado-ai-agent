package com.github.rony.estado.config;

import com.github.rony.estado.ask.EstadoTools;
import com.github.rony.estado.ask.SystemPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, EstadoTools estadoTools) {
        return builder
                .defaultSystem(SystemPrompt.TEXT)
                .defaultTools(estadoTools)
                .build();
    }
}
