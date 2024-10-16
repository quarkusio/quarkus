package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.beans.Charlie;
import io.quarkus.test.component.beans.MyComponent;

public class DependencyMockingTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = QuarkusComponentTestExtension.builder()
            .addComponentClasses(MyComponent.class)
            // this config property is injected into MyComponent and the value is used in the ping() method
            .configProperty("foo", "BAR")
            .build();

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
