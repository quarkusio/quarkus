package io.quarkus.arc.test.observers.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class ObserverNotificationTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StringObserver.class);

    @Test
    public void testContextualInstanceIsUsed() {
        assertEquals(0, StringObserver.CONSTRUCTED.get());
        assertEquals(0, StringObserver.NOTIFIED.get());
        Arc.container().beanManager().getEvent().fire("hello");
        assertEquals(1, StringObserver.CONSTRUCTED.get());
        assertEquals(1, StringObserver.NOTIFIED.get());
    }

    @ApplicationScoped
    static class StringObserver {

        private static final AtomicInteger CONSTRUCTED = new AtomicInteger();
        private static final AtomicInteger NOTIFIED = new AtomicInteger();

        StringObserver() {
            CONSTRUCTED.incrementAndGet();
        }

        void observeString(@Observes String value) {
            NOTIFIED.incrementAndGet();
        }

    }

}
