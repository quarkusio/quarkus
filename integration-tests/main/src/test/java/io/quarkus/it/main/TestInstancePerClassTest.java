package io.quarkus.it.main;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestInstancePerClassTest {

    @ConfigProperty(name = "web-message")
    String message;

    @Test
    void testInjectionWorksProperly() {
        Assertions.assertNotNull(message);
    }
}
