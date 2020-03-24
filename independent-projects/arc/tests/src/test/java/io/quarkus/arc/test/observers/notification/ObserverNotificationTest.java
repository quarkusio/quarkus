package io.quarkus.arc.test.observers.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ObserverNotificationTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StringObserver.class);

    @Test
    public void testContextualInstanceIsUsed() {
        assertEquals(0, StringObserver.CONSTRUCTED.get());
        assertEquals(0, StringObserver.NOTIFIED.get());
        Arc.container().beanManager().fireEvent("hello");
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
