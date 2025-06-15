package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ApplicationPropertiesConfigSourceTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = QuarkusComponentTestExtension.builder()
            .configProperty("org.acme.bar", "GRUT").build();

    @Inject
    Component component;

    @Test
    public void testComponent() {
        assertEquals("rocket", component.foo);
        assertEquals("GRUT", component.bar);
    }

    @Singleton
    public static class Component {

        @ConfigProperty(name = "org.acme.foo")
        String foo;

        @ConfigProperty(name = "org.acme.bar")
        String bar;
    }

}
