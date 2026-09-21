package com.github.rony.estado.exception;

public class InvalidInputException extends RuntimeException {

    private final ErrorCode errorCode;

    public InvalidInputException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
