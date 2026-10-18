package io.quarkus.test.component.nested;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import io.quarkus.test.component.beans.Charlie;
import io.quarkus.test.component.beans.MyComponent;

@QuarkusComponentTest
@TestConfigProperty(key = "foo", value = "BAR")
public class NestedNestedTest {

    @Inject
    MyComponent myComponent;

    @InjectMock
    Charlie charlie;

    @Test
    public void testPing() {
        Mockito.when(charlie.ping()).thenReturn("foo");
        assertEquals("foo and BAR", myComponent.ping());
    }

    @Nested
    class Nested1 {

        @Test
        public void testPing1() {
            Mockito.when(charlie.ping()).thenReturn("baz");
            assertEquals("baz and BAR", myComponent.ping());
        }

        @Nested
        class Nested2 {

            @Test
            @TestConfigProperty(key = "foo", value = "RAB")
            public void testPing2() {
                Mockito.when(charlie.ping()).thenReturn("baz");
                assertEquals("baz and RAB", myComponent.ping());
            }
        }
    }
}
