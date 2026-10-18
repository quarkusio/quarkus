package io.quarkus.it.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.vertx.http.HttpServer;
import io.smallrye.config.Config;

@QuarkusTest
class HttpServerTest {
    HttpServer httpServer;
    ValueRegistry valueRegistry;
    Config config;

    @Test
    void httpServer(HttpServer httpServer) {
        assertEquals(8081, httpServer.getPort());
        assertEquals(8081, httpServer.getLocalBaseUri().getPort());
        assertEquals(8081, this.httpServer.getPort());
        assertEquals(8081, this.httpServer.getLocalBaseUri().getPort());
    }

    @Test
    void valueRegistry(ValueRegistry valueRegistry) {
        assertEquals(8081, valueRegistry.get(HttpServer.HTTP_PORT));
        assertEquals(8081, valueRegistry.get(HttpServer.LOCAL_BASE_URI).getPort());
        assertEquals(8081, this.valueRegistry.get(HttpServer.HTTP_PORT));
        assertEquals(8081, this.valueRegistry.get(HttpServer.LOCAL_BASE_URI).getPort());
    }

    @Test
    void config(Config config) {
        assertEquals(8081, config.getValue("quarkus.http.port", int.class));
        assertEquals(8081, this.config.getValue("quarkus.http.port", int.class));
    }
}
