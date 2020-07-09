package io.quarkus.vertx.http.runtime.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vertx.core.http.HttpVersion;

class RequestLogMessageFormatterTest {

    @Test
    void format() {
        final RequestLogMessageFormatter formatter = new RequestLogMessageFormatter();

        final Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", "any-cookie");
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Connection", "keep-alive");

        final RequestLogMessage requestLogMessage = RequestLogMessage.builder()
                .headers(headers)
                .method("GET")
                .path("/quarkus")
                .version(HttpVersion.HTTP_1_1)
                .build();

        final String expectedFormat = "GET /quarkus HTTP/1.1" + System.lineSeparator()
                + "Cookie: any-cookie" + System.lineSeparator()
                + "Accept: application/json, text/plain, */*" + System.lineSeparator()
                + "Connection: keep-alive";

        assertEquals(expectedFormat, formatter.format(requestLogMessage));
    }
}
