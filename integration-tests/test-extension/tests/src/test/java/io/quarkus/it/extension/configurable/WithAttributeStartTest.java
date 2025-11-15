package io.quarkus.it.extension.configurable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.quarkus.it.extension.Counter;
import io.quarkus.test.junit.QuarkusTest;

@CustomResourceWithAttribute(value = "foo")
@QuarkusTest
public class WithAttributeStartTest {

    @Test
    public void test1() {
        assertEquals("foo", ConfigProvider.getConfig().getValue("attributeValue", String.class));
        assertTrue(Counter.startCounter.get() <= 1);
    }

    @Test
    public void test2() {
        assertTrue(Counter.startCounter.get() <= 1);
    }

}
