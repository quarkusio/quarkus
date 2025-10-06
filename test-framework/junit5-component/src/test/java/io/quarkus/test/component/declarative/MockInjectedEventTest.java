package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class MockInjectedEventTest {

    @Inject
    Emitter emitter;

    @InjectMock
    Event<AtomicInteger> event;

    @Test
    public void testEvent() {
        emitter.register();
        emitter.register();
        assertEquals(2, Emitter.COUNTER.get());
        Mockito.verify(event, Mockito.times(2)).fire(ArgumentMatchers.any());
    }

    @ApplicationScoped
    public static class Emitter {

        static final AtomicInteger COUNTER = new AtomicInteger();

        @Inject
        Event<AtomicInteger> event;

        void register() {
            COUNTER.incrementAndGet();
            event.fire(new AtomicInteger());
        }

    }

}
