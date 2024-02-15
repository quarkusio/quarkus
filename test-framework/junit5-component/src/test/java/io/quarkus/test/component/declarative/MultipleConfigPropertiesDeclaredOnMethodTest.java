package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import io.quarkus.test.component.beans.MultiPropComponent;

@QuarkusComponentTest
public class MultipleConfigPropertiesDeclaredOnMethodTest {

    @Inject
    MultiPropComponent component;

    @TestConfigProperty(key = "foo", value = "BAR")
    @TestConfigProperty(key = "bar", value = "BAZ")
    @Test
    public void testPing() {
        assertEquals("BARBAZ", component.getFooBar());
    }

}
