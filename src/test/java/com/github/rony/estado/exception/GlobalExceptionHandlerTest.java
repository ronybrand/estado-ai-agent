package com.github.rony.estado.exception;

import com.github.rony.estado.observability.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.remove(CorrelationIdFilter.MDC_KEY);
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

    @Test
    void shouldEchoRequestIdFromMdcInUpstreamFailureResponse() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "traced-id");
        UpstreamServiceException exception = new UpstreamServiceException(
                ErrorCode.ASK_02_UPSTREAM_FAILURE, "Falha ao consultar a API de estados", new RuntimeException("boom"));

        ResponseEntity<ErrorResponse> response = handler.handleUpstreamFailure(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().requestId()).isEqualTo("traced-id");
    }

    @Test
    void shouldReturnNullRequestIdWhenMdcIsEmpty() {
        UpstreamServiceException exception = new UpstreamServiceException(
                ErrorCode.ASK_02_UPSTREAM_FAILURE, "Falha ao consultar a API de estados", new RuntimeException("boom"));

        ResponseEntity<ErrorResponse> response = handler.handleUpstreamFailure(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().requestId()).isNull();
    }

    @Test
    void shouldMapUnexpectedExceptionTo500WithStandardErrorCode() {
        RuntimeException exception = new RuntimeException("algo inesperado explodiu");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpectedFailure(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ASK-99");
    }

    @Test
    void shouldNotLeakInternalExceptionMessageInGenericHandler() {
        RuntimeException exception = new RuntimeException("stacktrace sensivel com detalhe interno");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpectedFailure(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).doesNotContain("stacktrace sensivel");
    }

    @Test
    void shouldEchoRequestIdFromMdcInUnexpectedFailureResponse() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "traced-id");
        RuntimeException exception = new RuntimeException("boom");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpectedFailure(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().requestId()).isEqualTo("traced-id");
    }
}
