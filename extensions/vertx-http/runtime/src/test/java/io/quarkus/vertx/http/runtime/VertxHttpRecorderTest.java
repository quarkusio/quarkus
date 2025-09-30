package io.quarkus.vertx.http.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class VertxHttpRecorderTest {

    @Test
    void testListeningOnIpv6Any() throws IOException {
        StringBuilder builder = new StringBuilder();
        VertxHttpRecorder.appendListeningMessage(builder, "https", "::", 123);
        assertEquals("all addresses including https://localhost:123", builder.toString());
    }

    @Test
    void testListeningOnIpv4Any() throws IOException {
        StringBuilder builder = new StringBuilder();
        VertxHttpRecorder.appendListeningMessage(builder, "http", "0.0.0.0", 123);
        assertEquals("all addresses including http://localhost:123", builder.toString());
    }

    @Test
    void testListeningOnHost() throws IOException {
        StringBuilder builder = new StringBuilder();
        VertxHttpRecorder.appendListeningMessage(builder, "http", "localhost4", 8080);
        assertEquals("http://localhost4:8080", builder.toString());
    }

}
