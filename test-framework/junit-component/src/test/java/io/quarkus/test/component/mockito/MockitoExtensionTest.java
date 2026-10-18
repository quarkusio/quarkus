package io.quarkus.test.component.mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class MockitoExtensionTest {

    // Bar - component under test, real bean
    // Baz - mock of the synthetic bean registered to satisfy Bar#baz
    // Foo - plain Mockito mock
    @ExtendWith(MockitoExtension.class)
    @Test
    public void testInjectMock(Bar bar, @InjectMock Baz baz, @Mock Foo foo) {
        Mockito.when(foo.pong()).thenReturn(false);
        Mockito.when(baz.ping()).thenReturn(foo);
        assertFalse(bar.ping().pong());
    }

    @Singleton
    public static class Bar {

        @Inject
        Baz baz;

        Foo ping() {
            return baz.ping();
        }

    }

    public static class Baz {

        Foo ping() {
            return null;
        }

    }

    public static class Foo {

        boolean pong() {
            return true;
        }
    }

}
