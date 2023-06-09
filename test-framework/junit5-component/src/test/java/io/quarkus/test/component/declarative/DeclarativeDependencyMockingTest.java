package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import io.quarkus.test.component.beans.Charlie;
import io.quarkus.test.component.beans.MyComponent;

@QuarkusComponentTest
@TestConfigProperty(key = "foo", value = "BAR")
public class DeclarativeDependencyMockingTest {

    @Inject
    MyComponent myComponent;

    @InjectMock
    Charlie charlie;

    @Test
    public void testPing1() {
        Mockito.when(charlie.ping()).thenReturn("foo");
        assertEquals("foo and BAR", myComponent.ping());
    }

    @Test
    public void testPing2() {
        Mockito.when(charlie.ping()).thenReturn("baz");
        assertEquals("baz and BAR", myComponent.ping());
    }

}
