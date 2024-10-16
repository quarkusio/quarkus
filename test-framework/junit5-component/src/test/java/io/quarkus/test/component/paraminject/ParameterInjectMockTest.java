package io.quarkus.test.component.paraminject;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class ParameterInjectMockTest {

    // Foo is mocked even if it's not a dependency of a tested component
    @Test
    public void testInjectMock(@InjectMock MyFoo foo) {
        Mockito.when(foo.ping()).thenReturn(false);
        assertFalse(foo.ping());
    }

    public static class MyFoo {

        boolean ping() {
            return true;
        }
    }

}
