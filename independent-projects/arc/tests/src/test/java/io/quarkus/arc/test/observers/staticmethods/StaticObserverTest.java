package io.quarkus.arc.test.observers.staticmethods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StaticObserverTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StringObserver.class, Fool.class);

    @Test
    public void testObserver() {
        assertEquals(0, StringObserver.EVENTS.size());
        assertFalse(Fool.DESTROYED.get());
        Arc.container().beanManager().getEvent().fire("hello");
        assertEquals(1, StringObserver.EVENTS.size());
        assertEquals("hello", StringObserver.EVENTS.get(0));
        assertTrue(Fool.DESTROYED.get());
    }

    static class StringObserver {

        static final List<String> EVENTS = new CopyOnWriteArrayList<>();

        static void observeString(@Observes String value, Fool fool) {
            assertNotNull(fool);
            EVENTS.add(value);
        }

    }

    @Dependent
    static class Fool {

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }
    }

}
