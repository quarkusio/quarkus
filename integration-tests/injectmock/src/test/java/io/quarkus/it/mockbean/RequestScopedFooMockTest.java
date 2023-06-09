package io.quarkus.it.mockbean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class RequestScopedFooMockTest {

    @InjectMock
    RequestScopedFoo foo;

    @Test
    void testMock() {
        when(foo.ping()).thenReturn("pong");
        assertEquals("pong", foo.ping());
        assertFalse(RequestScopedFoo.CONSTRUCTED.get());
    }

}
