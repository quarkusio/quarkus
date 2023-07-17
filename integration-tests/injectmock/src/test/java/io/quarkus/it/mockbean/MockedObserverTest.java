package io.quarkus.it.mockbean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
class MockedObserverTest {

    @InjectMock
    AlphaObserver observer;

    @Inject
    Event<AtomicReference<BigDecimal>> event;

    @Test
    public void testMockedObserverNotNotified() {
        Mockito.when(observer.test()).thenReturn(false);
        assertFalse(observer.test());
        AtomicReference<BigDecimal> payload = new AtomicReference<BigDecimal>(BigDecimal.ZERO);
        event.fire(payload);
        // BravoObserver is not mocked
        assertEquals(BigDecimal.ONE, payload.get());
    }

}
