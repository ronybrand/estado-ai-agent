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

    AskResponse ask(AskRequest request) {
        log.info("Consultando modelo de IA, tamanho da pergunta={}", request.question().length());
        try {
            String answer = chatClient.prompt()
                    .user(request.question())
                    .call()
                    .content();
            log.info("Resposta do modelo de IA obtida com sucesso");
            return new AskResponse(answer);
        } catch (RestClientException e) {
            log.warn("Falha ao consultar a API de estados via modelo de IA", e);
            throw new UpstreamServiceException(
                    ErrorCode.ASK_02_UPSTREAM_FAILURE, "Falha ao consultar a API de estados", e);
        }
    }
}
