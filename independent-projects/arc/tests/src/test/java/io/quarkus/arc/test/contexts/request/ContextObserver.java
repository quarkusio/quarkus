package io.quarkus.arc.test.contexts.request;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class ContextObserver {

    public static int initializedObserved = 0;
    public static int beforeDestroyedObserved = 0;
    public static int destroyedObserved = 0;

    public static void reset() {
        initializedObserved = 0;
        beforeDestroyedObserved = 0;
        destroyedObserved = 0;
    }

    public void observeContextInit(@Observes @Initialized(RequestScoped.class) Object event) {
        initializedObserved++;
    }

    public void observeContextBeforeDestroyed(@Observes @BeforeDestroyed(RequestScoped.class) Object event) {
        beforeDestroyedObserved++;
    }

    public void observeContextDestroyed(@Observes @Destroyed(RequestScoped.class) Object event) {
        destroyedObserved++;
    }
}
