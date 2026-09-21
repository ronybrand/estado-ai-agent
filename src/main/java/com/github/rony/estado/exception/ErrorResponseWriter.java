package com.github.rony.estado.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

// Ponto unico para filtros de servlet (fora do RestControllerAdvice, que so
// cobre excecoes lancadas dentro do dispatch do Spring MVC) responderem erro
// no mesmo contrato JSON (ErrorResponse) usado pelo GlobalExceptionHandler,
// em vez de texto puro.
public final class ErrorResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ErrorResponseWriter() {
    }

    public static void write(HttpServletResponse response, HttpStatus status, ErrorCode errorCode,
            String message, String requestId) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                OBJECT_MAPPER.writeValueAsString(new ErrorResponse(errorCode.code(), message, requestId)));
    }
}
