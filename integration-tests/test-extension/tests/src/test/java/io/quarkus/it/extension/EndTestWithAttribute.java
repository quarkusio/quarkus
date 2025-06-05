package io.quarkus.it.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@CustomResourceWithAttribute(value = "bar")
@QuarkusTest
public class EndTestWithAttribute {

    @Inject
    @ConfigProperty(name = "attributeValue")
    String attributeValue;

    @Test
    public void test1() {
        assertEquals("bar", attributeValue);
        assertTrue(Counter.endCounter.get() <= 1);
    }

    @Test
    public void test2() {
        assertTrue(Counter.endCounter.get() <= 1);
    }

}
