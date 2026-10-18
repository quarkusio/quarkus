package io.quarkus.test.component.nested;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
public class NestedParamInjectTest {

    @InjectMock
    Charlie charlie;

    @Nested
    class Nested1 {

        @Test
        public void testPing1(MyComponent myComponent) {
            Mockito.when(charlie.ping()).thenReturn("baz");
            assertEquals("baz and BAR", myComponent.ping());
        }
    }
}
