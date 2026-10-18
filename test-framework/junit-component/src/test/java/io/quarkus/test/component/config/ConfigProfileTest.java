package io.quarkus.test.component.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@QuarkusComponentTest
public class ConfigProfileTest {

    @Inject
    Foo foo;

    @TestConfigProperty(key = "%test.app.baz", value = "boom")
    @TestConfigProperty(key = "%dev.app.bar", value = "boom")
    @Test
    public void testMapping() {
        // the configured value for AppConfig.baz() is used
        assertEquals("boom", foo.config.baz());
        // the default value for AppConfig.bar() is used
        assertEquals("loom", foo.config.bar());
    }

    @Singleton
    public static class Foo {

        @Inject
        AppConfig config;
    }

    @ConfigMapping(prefix = "app")
    interface AppConfig {

        @WithDefault("loom")
        String baz();

        @WithDefault("loom")
        String bar();

    }
}
