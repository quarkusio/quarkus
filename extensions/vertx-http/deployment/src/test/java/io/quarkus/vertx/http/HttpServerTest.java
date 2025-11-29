package io.quarkus.vertx.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.HttpServer;

public class HttpServerTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest();

    @Inject
    HttpServer webServer;

    @Test
    void ports() {
        assertTrue(webServer.getPort() > 0);
        assertEquals(-1, webServer.getSecurePort());
        assertEquals(-1, webServer.getManagementPort());
    }
}
