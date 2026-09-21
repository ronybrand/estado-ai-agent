package com.github.rony.estado.ask;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class EstadoTools {

    private final RestClient restClient;

    public EstadoTools(RestClient restClient) {
        this.restClient = restClient;
    }

    @Tool(description = "Lista os estados brasileiros cadastrados com paginação (retorna a página de estados). "
            + "O page padrão é 0 e o size padrão é 20 (máximo permitido: 100).")
    public String listEstados(int page, int size) {
        PaginationParams params = PaginationParams.of(page, size);

        return restClient.get()
                .uri(UriComponentsBuilder.fromPath("/estado/paginado")
                        .queryParam("page", params.page())
                        .queryParam("size", params.size())
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
