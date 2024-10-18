package io.quarkus.vertx.http.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.Http;

public class HttpTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest();

    @Inject
    Http http;

    @Test
    void http() {
        assertNotNull(http.host());
        assertTrue(http.port() > 0);
        assertTrue(http.sslPort() > 0);
        assertNotNull(Http.get().host());
        assertTrue(Http.get().port() > 0);
        assertTrue(Http.get().sslPort() > 0);
    }
}
