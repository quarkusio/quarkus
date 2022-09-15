package io.quarkus.arc.test.observers.inheritance.generated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

// see https://github.com/quarkusio/quarkus/issues/23888
public class GeneratedObserverClassNotSharedTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Alpha.class, Bravo.class);

    static final Set<Alpha> OBSERVING_ALPHAS = new CopyOnWriteArraySet<>();

    @Test
    public void testBeanInstancesAreShared() {
        OBSERVING_ALPHAS.clear();
        Event<Payload> event = Arc.container().beanManager().getEvent().select(Payload.class);
        // Fire two events and verify that only one instance of Alpha and Bravo was used
        event.fire(new Payload());
        event.fire(new Payload());
        assertEquals(2, OBSERVING_ALPHAS.size());
        // Also verify that the observing instances are the same as injectable instances
        for (Alpha alpha : Arc.container().select(Alpha.class)) {
            if (alpha instanceof ClientProxy) {
                alpha = (Alpha) ((ClientProxy) alpha).arc_contextualInstance();
            }
            assertTrue(OBSERVING_ALPHAS.contains(alpha), alpha + " was not used to observe the event");
        }
    }

    @Singleton
    public static class Alpha {

        void observe(@Observes Payload payload) {
            OBSERVING_ALPHAS.add(this);
        }

    }

    @ApplicationScoped
    public static class Bravo extends Alpha {

    }

    public static class Payload {

    }

}
