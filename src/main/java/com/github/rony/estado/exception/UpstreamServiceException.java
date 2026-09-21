package com.github.rony.estado.exception;

public class UpstreamServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    public UpstreamServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
