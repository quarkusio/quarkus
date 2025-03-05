package io.quarkus.test.component.nested;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import io.quarkus.test.component.beans.Charlie;
import io.quarkus.test.component.beans.MyComponent;

@QuarkusComponentTest
@TestConfigProperty(key = "foo", value = "BAR")
public class TopPerMethodNestedPerClassTest {

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
    @TestInstance(Lifecycle.PER_CLASS)
    class Nested1 {

        @Test
        public void testPing1() {
            Mockito.when(charlie.ping()).thenReturn("baz");
            assertEquals("baz and BAR", myComponent.ping());
        }
    }

}
