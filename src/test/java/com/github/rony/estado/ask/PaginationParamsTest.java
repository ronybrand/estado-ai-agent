package com.github.rony.estado.ask;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationParamsTest {

    @Test
    void shouldKeepValidPageAndSize() {
        PaginationParams params = PaginationParams.of(2, 50);

        assertThat(params.page()).isEqualTo(2);
        assertThat(params.size()).isEqualTo(50);
    }

    @Test
    void shouldClampNegativePageToZero() {
        PaginationParams params = PaginationParams.of(-5, 20);

        assertThat(params.page()).isZero();
    }

    @Test
    void shouldUseDefaultSizeWhenSizeIsZero() {
        PaginationParams params = PaginationParams.of(0, 0);

        assertThat(params.size()).isEqualTo(PaginationParams.DEFAULT_PAGE_SIZE);
    }

    @Test
    void shouldUseDefaultSizeWhenSizeIsNegative() {
        PaginationParams params = PaginationParams.of(0, -10);

        assertThat(params.size()).isEqualTo(PaginationParams.DEFAULT_PAGE_SIZE);
    }

    @Test
    void shouldClampSizeAboveMaxToMax() {
        PaginationParams params = PaginationParams.of(0, 500);

        assertThat(params.size()).isEqualTo(PaginationParams.MAX_PAGE_SIZE);
    }

    @Test
    void shouldKeepSizeAtExactMax() {
        PaginationParams params = PaginationParams.of(0, PaginationParams.MAX_PAGE_SIZE);

        assertThat(params.size()).isEqualTo(PaginationParams.MAX_PAGE_SIZE);
    }
}
