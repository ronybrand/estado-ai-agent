package com.github.rony.estado.ask;

import com.github.rony.estado.exception.ErrorCode;
import com.github.rony.estado.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class AskService {

    private static final Logger log = LoggerFactory.getLogger(AskService.class);

    private final ChatClient chatClient;

    public AskService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    private static final String LEAK_REFUSAL_MESSAGE =
            "Nao posso compartilhar essa informacao. Posso ajudar com perguntas sobre os estados brasileiros.";

    AskResponse ask(AskRequest request) {
        log.info("Consultando modelo de IA, tamanho da pergunta={}", request.question().length());
        try {
            String answer = chatClient.prompt()
                    .user(request.question())
                    .call()
                    .content();
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
}
