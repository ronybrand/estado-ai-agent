package com.github.rony.estado.exception;

public enum ErrorCode {

    ASK_01_INVALID_INPUT("ASK-01"),
    ASK_02_UPSTREAM_FAILURE("ASK-02"),
    ASK_03_UNAUTHORIZED("ASK-03"),
    ASK_04_RATE_LIMITED("ASK-04"),
    ASK_99_INTERNAL_ERROR("ASK-99");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
