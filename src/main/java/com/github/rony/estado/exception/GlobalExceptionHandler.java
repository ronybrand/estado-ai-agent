package com.github.rony.estado.exception;

import com.github.rony.estado.observability.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    // O front (extrai-request-id-erro.ts) le requestId do CORPO do erro, nao
    // so do header X-Request-Id - sem isso, o usuario nao consegue
    // correlacionar um erro do /ask com o proprio log, diferente do que ja
    // acontece nos erros da API de estados (ErrorResponseDto).
    private String currentRequestId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}
