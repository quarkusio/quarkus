package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class MyComponentTest {

    @Inject
    @ConfigProperty // name and default value are nonbinding
    String myProperty;

    @Test
    public void testProperty() {
        assertEquals("foo", myProperty);
    }

    @Singleton
    static class PropertyProducer {

        // This producer would normally break all @QuarkusTest in the test suite
        // However, since it's a static nested class declared on a @QuarkusComponentTest it's excluded from the bean discovery
        @Produces
        @ConfigProperty
        String myString() {
            return "foo";
        }

    }

}
