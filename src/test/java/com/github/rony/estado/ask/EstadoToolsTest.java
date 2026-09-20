package com.github.rony.estado.ask;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EstadoToolsTest {

    @Test
    void shouldListEstados() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("{\"content\":[{\"nome\":\"Paraná\"}]}");

        EstadoTools tools = new EstadoTools(restClient);
        String response = tools.listEstados(0, 20);

        assertThat(response).contains("Paraná");
    }

    @Test
    void shouldGetEstadoById() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("{\"nome\":\"Santa Catarina\"}");

        EstadoTools tools = new EstadoTools(restClient);
        String response = tools.getEstadoById(42);

        assertThat(response).contains("Santa Catarina");
    }
}
