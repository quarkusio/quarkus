package io.quarkus.arc.test.observers.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.ObserverMethod;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class ManualNotifyInvocationTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StringObserver.class);

    @Test
    public void testManualNotifyInvocation() {
        assertEquals(0, StringObserver.NOTIFIED.get());
        Arc.container().beanManager().getEvent().fire("hello");
        assertEquals(1, StringObserver.NOTIFIED.get());

        Set<ObserverMethod<? super String>> foundOM = Arc.container().beanManager().resolveObserverMethods("foo");
        assertEquals(1, foundOM.size());
        foundOM.iterator().next().notify("test");
        assertEquals(2, StringObserver.NOTIFIED.get());
    }

    @ApplicationScoped
    static class StringObserver {
        private static final AtomicInteger NOTIFIED = new AtomicInteger();

        void observeString(@Observes String value) {
            NOTIFIED.incrementAndGet();
        }
    }
}
