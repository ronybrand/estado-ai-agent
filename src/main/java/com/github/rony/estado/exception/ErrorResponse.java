package com.github.rony.estado.exception;

public record ErrorResponse(String code, String message, String requestId) {
}
