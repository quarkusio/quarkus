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
public class ConfigPropertyDeclaredOnMethodTest {

    @Inject
    MyComponent myComponent;

    @InjectMock
    Charlie charlie;

    @TestConfigProperty(key = "foo", value = "BAR")
    @Test
    public void testPing1() {
        Mockito.when(charlie.ping()).thenReturn("1");
        assertEquals("1 and BAR", myComponent.ping());
    }

    @TestConfigProperty(key = "foo", value = "BAZ")
    @Test
    public void testPing2() {
        Mockito.when(charlie.ping()).thenReturn("2");
        assertEquals("2 and BAZ", myComponent.ping());
    }

}
