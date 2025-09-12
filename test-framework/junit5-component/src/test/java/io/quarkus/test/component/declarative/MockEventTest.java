package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class MockEventTest {

    @InjectMock
    Event<AtomicInteger> event;

    @Test
    public void testEvent() {
        Mockito.doAnswer(invocation -> {
            Object arg0 = invocation.getArgument(0);
            if (arg0 instanceof AtomicInteger integer) {
                integer.set(integer.get() + 15);
            }
            return null;
        }).when(event).fire(Mockito.any(AtomicInteger.class));

        AtomicInteger payload = new AtomicInteger();
        event.fire(payload);

        // AtomicIntegerObserver is not notified
        assertEquals(15, payload.get());
    }

    // component under test - nested static classes are added automatically
    public static class AtomicIntegerObserver {

        void onInt(@Observes AtomicInteger integer) {
            // should not be notified
            integer.set(integer.get() + 5);
        }

    }

}
