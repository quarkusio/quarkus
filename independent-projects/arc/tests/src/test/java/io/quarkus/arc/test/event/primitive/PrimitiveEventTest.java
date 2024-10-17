package io.quarkus.arc.test.event.primitive;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class PrimitiveEventTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Observer.class, Producer.class);

    @Test
    public void test() {
        assertFalse(Observer.primitiveNotified);
        assertFalse(Observer.wrapperNotified);

        Arc.container().select(Producer.class).get().produce();

        assertTrue(Observer.primitiveNotified);
        assertTrue(Observer.wrapperNotified);
    }

    @Singleton
    static class Observer {
        static boolean primitiveNotified = false;
        static boolean wrapperNotified = false;

        void observePrimitive(@Observes int event) {
            primitiveNotified = true;
        }

        void observeWrapper(@Observes Integer event) {
            wrapperNotified = true;
        }
    }

    @Singleton
    static class Producer {
        @Inject
        Event<Integer> event;

        void produce() {
            event.fire(42);
        }
    }
}
