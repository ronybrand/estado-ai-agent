package com.github.rony.estado.ask;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class EstadoTools {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final RestClient restClient;

    public EstadoTools(RestClient restClient) {
        this.restClient = restClient;
    }

    @Tool(description = "Lista os estados brasileiros cadastrados com paginação (retorna a página de estados). "
            + "O page padrão é 0 e o size padrão é 20 (máximo permitido: 100).")
    public String listEstados(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        return restClient.get()
                .uri(UriComponentsBuilder.fromPath("/estado/paginado")
                        .queryParam("page", safePage)
                        .queryParam("size", safeSize)
                        .build().toUriString())
                .retrieve()
                .body(String.class);
    }

    @Tool(description = "Busca os detalhes de um estado específico pelo seu ID numérico.")
    public String getEstadoById(long id) {
        return restClient.get()
                .uri("/estado/{id}", id)
                .retrieve()
                .body(String.class);
    }
}
