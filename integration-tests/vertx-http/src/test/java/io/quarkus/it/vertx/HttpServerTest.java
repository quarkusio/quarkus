package io.quarkus.it.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.HttpServer;

@QuarkusTest
class HttpServerTest {
    @Test
    void httpServer(HttpServer httpServer) {
        assertEquals(8081, httpServer.getPort());
        assertEquals(8081, httpServer.getLocalBaseUri().getPort());
    }
}
