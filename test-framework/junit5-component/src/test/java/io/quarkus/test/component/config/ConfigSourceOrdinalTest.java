package io.quarkus.test.component.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;

@QuarkusComponentTest(configSourceOrdinal = 275)
@TestConfigProperty(key = "foo", value = "baz")
public class ConfigSourceOrdinalTest {

    @BeforeAll
    static void beforeAll() {
        System.setProperty("foo", "bar");
    }

    @AfterAll
    static void afterAll() {
        System.clearProperty("foo");
    }

    @Inject
    FooConsumer consumer;

    @Test
    public void testOrdinal() {
        assertEquals("bar", consumer.foo);
    }

    @Singleton
    public static class FooConsumer {

        @ConfigProperty(name = "foo")
        String foo;

    }
}
