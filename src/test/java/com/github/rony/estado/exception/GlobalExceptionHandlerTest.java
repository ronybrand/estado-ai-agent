package com.github.rony.estado.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapInvalidInputExceptionTo400() {
        InvalidInputException exception = new InvalidInputException(ErrorCode.ASK_01_INVALID_INPUT, "question e obrigatoria");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidInput(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ASK-01");
        assertThat(response.getBody().message()).isEqualTo("question e obrigatoria");
    }

    @Test
    void shouldMapUpstreamServiceExceptionTo502() {
        UpstreamServiceException exception = new UpstreamServiceException(
                ErrorCode.ASK_02_UPSTREAM_FAILURE, "Falha ao consultar a API de estados", new RuntimeException("boom"));

        ResponseEntity<ErrorResponse> response = handler.handleUpstreamFailure(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ASK-02");
        assertThat(response.getBody().message()).isEqualTo("Falha ao consultar a API de estados");
    }
}
