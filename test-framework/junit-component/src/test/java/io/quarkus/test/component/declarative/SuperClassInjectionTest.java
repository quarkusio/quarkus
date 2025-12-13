package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;

@QuarkusComponentTest
@TestConfigProperty(key = "foo", value = "BAR")
public class SuperClassInjectionTest extends SuperTest {

    @Test
    public void testPing() {
        Mockito.when(charlie.ping()).thenReturn("foo");
        assertEquals("foo and BAR", myComponent.ping());
    }

}
