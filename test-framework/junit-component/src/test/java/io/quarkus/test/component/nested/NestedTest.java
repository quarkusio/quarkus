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
import io.quarkus.test.component.beans.MySimpleComponent;

@QuarkusComponentTest
@TestConfigProperty(key = "foo", value = "BAR")
public class NestedTest {

    @Inject
    MyComponent myComponent;

    @Inject
    MySimpleComponent mySimpleComponent;

    @InjectMock
    Charlie charlie;

    @Test
    public void testPing() {
        Mockito.when(charlie.ping()).thenReturn("foo");
        assertEquals("foo and BAR", myComponent.ping());
        assertEquals("foo", mySimpleComponent.ping());
    }

    @TestConfigProperty(key = "foo", value = "BANG") // declared on nested test class -> ignored
    @Nested
    class Nested1 {

        @InjectMock
        Charlie charlie;

        @Test
        public void testPing1() {
            Mockito.when(charlie.ping()).thenReturn("baz");
            assertEquals("baz and BAR", myComponent.ping());
            assertEquals("foo", mySimpleComponent.ping());
        }
    }

    @Nested
    class Nested2 {

        @Inject
        MyComponent myComponent;

        @Test
        @TestConfigProperty(key = "foo", value = "RAB")
        public void testPing2() {
            Mockito.when(charlie.ping()).thenReturn("baz");
            assertEquals("baz and RAB", myComponent.ping());
            assertEquals("foo", mySimpleComponent.ping());
        }

        @Nested
        class Nested3 {

            @Test
            @TestConfigProperty(key = "foo", value = "AJAJ")
            public void testPing3() {
                Mockito.when(charlie.ping()).thenReturn("bazinga");
                assertEquals("bazinga and AJAJ", myComponent.ping());
                assertEquals("foo", mySimpleComponent.ping());
            }

            @Nested
            class Nested4 {

                @Test
                @TestConfigProperty(key = "foo", value = "JAJA")
                public void testPing3() {
                    Mockito.when(charlie.ping()).thenReturn("BAZINGA");
                    assertEquals("BAZINGA and JAJA", myComponent.ping());
                    assertEquals("foo", mySimpleComponent.ping());
                }

            }

        }
    }
}
