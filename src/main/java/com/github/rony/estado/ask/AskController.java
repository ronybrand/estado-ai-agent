package com.github.rony.estado.ask;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ask")
public class AskController {

    private static final int MAX_QUESTION_LENGTH = 1000;

    private final ChatClient chatClient;

    public AskController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping
    public AskResponse ask(@RequestBody AskRequest request) {
        String question = request.question();
        if (question == null || question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question e obrigatoria");
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "question excede o tamanho maximo de " + MAX_QUESTION_LENGTH + " caracteres");
        }

        try {
            String answer = chatClient.prompt()
                    .user(question)
                    .call()
                    .content();
            return new AskResponse(answer);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha ao consultar a API de estados", e);
        }
    }
}
