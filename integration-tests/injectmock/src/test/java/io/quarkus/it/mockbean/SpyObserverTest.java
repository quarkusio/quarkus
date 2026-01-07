package io.quarkus.it.mockbean;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;

@QuarkusTest
class SpyObserverTest {

    @InjectSpy
    SomeClass someClass;

    @Inject
    Event<AtomicReference<LongAdder>> event;

    @Test
    public void testMockedObserverNotNotified() {
        Mockito.verify(someClass, Mockito.times(0)).get();
        assertEquals("foo", someClass.get());
        AtomicReference<LongAdder> payload = new AtomicReference<>(new LongAdder());
        event.fire(payload);
        assertEquals(1L, payload.get().longValue());
    }

    @ApplicationScoped
    public static class SomeClass {

        public String get() {
            return "foo";
        }

        void onBigDecimal(@Observes AtomicReference<LongAdder> event) {
            event.get().increment();
        }
    }

}
