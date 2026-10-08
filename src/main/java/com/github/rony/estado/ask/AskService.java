package com.github.rony.estado.ask;

import com.github.rony.estado.exception.ErrorCode;
import com.github.rony.estado.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Service
public class AskService {

    private static final Logger log = LoggerFactory.getLogger(AskService.class);

    private final ChatClient chatClient;

    private final String modeloReserva;

    // Modelo usado quando o principal esta sem capacidade (503/429) ou lento
    // (timeout); vazio desliga o fallback.
    public AskService(ChatClient chatClient, @Value("${app.gemini.fallback-model:}") String modeloReserva) {
        this.chatClient = chatClient;
        this.modeloReserva = modeloReserva;
    }

    private static final String LEAK_REFUSAL_MESSAGE =
            "Nao posso compartilhar essa informacao. Posso ajudar com perguntas sobre os estados brasileiros.";

    AskResponse ask(AskRequest request) {
        log.info("Consultando modelo de IA, tamanho da pergunta={}", request.question().length());
        if (PromptInjectionGuard.isSuspicious(request.question())) {
            // Guarda de entrada deterministica: bloqueia as tentativas mais
            // comuns e baratas de prompt injection antes mesmo de consumir
            // uma chamada ao LLM. Nao substitui a guarda de saida (que cobre
            // vazamento efetivo de regras internas mesmo quando esta guarda
            // de entrada nao reconhece a frase usada).
            log.warn("Pergunta sinalizada como tentativa de prompt injection - bloqueada antes do LLM");
            return new AskResponse(LEAK_REFUSAL_MESSAGE);
        }
        try {
            String answer = consultarModelo(request.question());
            log.info("Resposta do modelo de IA obtida com sucesso");
            if (SystemPromptLeakGuard.isLeaking(answer)) {
                // Guarda de saida deterministica: um prompt injection bem
                // sucedido pode fazer o modelo obedecer e repetir as regras
                // internas na resposta, apesar da instrucao no proprio
                // system prompt para nunca fazer isso (defesa probabilistica).
                log.warn("Resposta do modelo de IA continha vazamento do system prompt - substituida");
                return new AskResponse(LEAK_REFUSAL_MESSAGE);
            }
            return new AskResponse(answer);
        } catch (RestClientException e) {
            log.warn("Falha ao consultar a API de estados via modelo de IA", e);
            throw new UpstreamServiceException(
                    ErrorCode.ASK_02_UPSTREAM_FAILURE, "Falha ao consultar a API de estados", e);
        }
    }

    private String consultarModelo(String pergunta) {
        try {
            return chatClient.prompt().user(pergunta).call().content();
        } catch (RuntimeException falhaPrincipal) {
            if (!StringUtils.hasText(modeloReserva) || !GeminiFailures.valeTentarOutroModelo(falhaPrincipal)) {
                throw falhaPrincipal;
            }
            log.warn("Modelo principal indisponivel ou lento, tentando o modelo reserva {}: {}",
                    modeloReserva, falhaPrincipal.getMessage());
            try {
                return chatClient.prompt()
                        .user(pergunta)
                        .options(GoogleGenAiChatOptions.builder().model(modeloReserva))
                        .call()
                        .content();
            } catch (RuntimeException falhaReserva) {
                falhaReserva.addSuppressed(falhaPrincipal);
                throw falhaReserva;
            }
        }
    }
}
