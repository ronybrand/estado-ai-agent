package com.github.rony.estado.exception;

import com.github.rony.estado.observability.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UpstreamServiceException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamFailure(UpstreamServiceException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(exception.errorCode().code(), exception.getMessage(), currentRequestId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailure(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Requisicao invalida");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ErrorCode.ASK_01_INVALID_INPUT.code(), message, currentRequestId()));
    }

    // Rede de seguranca para qualquer excecao nao mapeada explicitamente
    // acima (ex.: NPE, falha do proprio Spring AI): sem isso, o Spring Boot
    // devolveria seu formato de erro padrao em vez do ErrorResponse
    // consistente usado no resto da API. A mensagem original nunca vai pro
    // corpo da resposta (poderia vazar detalhe interno); fica so no log.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedFailure(Exception exception) {
        log.error("Erro inesperado ao processar requisicao", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ErrorCode.ASK_99_INTERNAL_ERROR.code(), "Erro interno inesperado", currentRequestId()));
    }

    // O front (extrai-request-id-erro.ts) le requestId do CORPO do erro, nao
    // so do header X-Request-Id - sem isso, o usuario nao consegue
    // correlacionar um erro do /ask com o proprio log, diferente do que ja
    // acontece nos erros da API de estados (ErrorResponseDto).
    private String currentRequestId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}
