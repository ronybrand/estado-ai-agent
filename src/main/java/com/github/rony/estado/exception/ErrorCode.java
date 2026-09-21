package com.github.rony.estado.exception;

public enum ErrorCode {

    ASK_01_INVALID_INPUT("ASK-01"),
    ASK_02_UPSTREAM_FAILURE("ASK-02");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
