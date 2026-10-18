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

    @InjectMock
    RequestScopedFooFromProducer foo2;

    @Test
    void testMock() {
        when(foo.ping()).thenReturn("pong");
        when(foo2.ping()).thenReturn("pong2");
        assertEquals("pong", foo.ping());
        assertEquals("pong2", foo2.ping());
        assertFalse(RequestScopedFoo.CONSTRUCTED.get());
        assertFalse(RequestScopedFooFromProducer.CONSTRUCTED.get());
    }

}
