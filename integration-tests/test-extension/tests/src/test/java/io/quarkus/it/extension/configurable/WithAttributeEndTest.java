package io.quarkus.it.extension.configurable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.it.extension.Counter;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.config.Config;

@CustomResourceWithAttribute(value = "bar")
@QuarkusTest
public class WithAttributeEndTest {

    @Test
    public void test1() {
        assertEquals("bar", Config.get().getValue("attributeValue", String.class));
        assertTrue(Counter.endCounter.get() <= 1);
    }

    @Test
    public void test2() {
        assertTrue(Counter.endCounter.get() <= 1);
    }

}
