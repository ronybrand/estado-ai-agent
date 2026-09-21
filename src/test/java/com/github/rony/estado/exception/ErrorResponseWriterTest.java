package com.github.rony.estado.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseWriterTest {

    @Test
    void shouldWriteJsonBodyWithStatusCodeAndMessage() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ErrorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, ErrorCode.ASK_03_UNAUTHORIZED, "Unauthorized", "traced-id");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("\"code\":\"ASK-03\"");
        assertThat(response.getContentAsString()).contains("\"message\":\"Unauthorized\"");
        assertThat(response.getContentAsString()).contains("\"requestId\":\"traced-id\"");
    }

    @Test
    void shouldWriteNullRequestIdWhenNotProvided() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ErrorResponseWriter.write(response, HttpStatus.TOO_MANY_REQUESTS, ErrorCode.ASK_04_RATE_LIMITED, "Too many requests", null);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("\"code\":\"ASK-04\"");
    }
}
