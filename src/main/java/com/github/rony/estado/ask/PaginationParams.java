package com.github.rony.estado.ask;

public record PaginationParams(int page, int size) {

    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 20;

    public static PaginationParams of(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return new PaginationParams(safePage, safeSize);
    }
}
