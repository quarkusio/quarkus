package io.quarkus.vertx.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.service.Address;
import io.quarkus.runtime.service.Services;
import io.quarkus.test.QuarkusExtensionTest;

class VertxHttpServiceTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest();

    @Test
    void vertxHttpService() {
        Address address = Services.resolve("quarkus://vertx-http");
        assertEquals("http://localhost:8081", address.uri().toString());
    }
}
