package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.beans.Charlie;
import io.quarkus.test.component.beans.MyComponent;

public class MockConfiguratorTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = QuarkusComponentTestExtension.builder().mock(Charlie.class)
            .createMockitoMock(charlie -> {
                Mockito.when(charlie.pong()).thenReturn("bar");
            }).configProperty("foo", "BAR").build();

    @Inject
    MyComponent myComponent;

    @InjectMock
    Charlie charlie;

    @Test
    public void testComponent() {
        when(charlie.ping()).thenReturn("foo");
        assertEquals("foo and BAR", myComponent.ping());
        assertEquals("bar and BAR", myComponent.pong());

        when(charlie.ping()).thenReturn("baz");
        assertEquals("baz and BAR", myComponent.ping());
    }

}
