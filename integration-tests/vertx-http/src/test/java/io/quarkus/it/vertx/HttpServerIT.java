package io.quarkus.it.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class HttpServerIT extends HttpServerTest {

    static int beforeAllPort;

    @BeforeAll
    static void configAvailableInBeforeAll() {
        beforeAllPort = ConfigProvider.getConfig().getValue("quarkus.http.port", Integer.class);
        assertTrue(beforeAllPort > 0);
    }

    @Test
    void beforeAllConfigMatchesTestConfig() {
        assertEquals(8081, beforeAllPort);
    }
}
