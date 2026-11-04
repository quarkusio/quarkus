package io.quarkus.it.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.smallrye.config.Config;

@QuarkusIntegrationTest
public class HttpServerIT extends HttpServerTest {

    static int beforeAllPort;

    @BeforeAll
    static void configAvailableInBeforeAll() {
        beforeAllPort = Config.get().getValue("quarkus.http.port", Integer.class);
        assertTrue(beforeAllPort > 0);
    }

    @Test
    void beforeAllConfigMatchesTestConfig() {
        assertEquals(8081, beforeAllPort);
    }
}
