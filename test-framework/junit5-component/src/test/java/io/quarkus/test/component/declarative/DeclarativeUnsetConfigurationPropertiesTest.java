package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.declarative.DeclarativeUnsetConfigurationPropertiesTest.Component;

@QuarkusComponentTest(value = Component.class, useDefaultConfigProperties = true)
public class DeclarativeUnsetConfigurationPropertiesTest {

    @Inject
    Component component;

    @Test
    public void testComponent() {
        assertNull(component.foo);
        assertFalse(component.bar);
        assertEquals(0, component.baz);
        assertNull(component.bazzz);
    }

    @Singleton
    public static class Component {

        @ConfigProperty(name = "foo")
        String foo;

        @ConfigProperty(name = "bar")
        boolean bar;

        @ConfigProperty(name = "baz")
        int baz;

        @ConfigProperty(name = "bazzz")
        Integer bazzz;

    }

}
