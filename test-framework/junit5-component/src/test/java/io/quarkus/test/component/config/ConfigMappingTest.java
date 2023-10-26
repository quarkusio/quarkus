package io.quarkus.test.component.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@QuarkusComponentTest
@TestConfigProperty(key = "foo.bar", value = "true")
@TestConfigProperty(key = "foo.log.rotate", value = "true")
public class ConfigMappingTest {

    @Inject
    Foo foo;

    @TestConfigProperty(key = "foo.oof", value = "boom")
    @Test
    public void testMapping() {
        assertTrue(foo.config.bar());
        assertTrue(foo.config.log().rotate());
        assertEquals("loom", foo.config.baz());
        assertEquals("boom", foo.config.oof());
    }

    @TestConfigProperty(key = "foo.oof", value = "boomboom")
    @Test
    public void testAnotherMapping() {
        assertTrue(foo.config.bar());
        assertTrue(foo.config.log().rotate());
        assertEquals("loom", foo.config.baz());
        assertEquals("boomboom", foo.config.oof());
    }

    @Singleton
    public static class Foo {

        @Inject
        FooConfig config;
    }

    @ConfigMapping(prefix = "foo")
    interface FooConfig {

        boolean bar();

        @WithDefault("loom")
        String baz();

        String oof();

        Log log();

        // nested mapping
        interface Log {
            boolean rotate();
        }
    }
}
