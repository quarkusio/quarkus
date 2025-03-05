package io.quarkus.arc.test.contexts.request;

import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;

@RequestScoped
public class RequestScopedObserver {

    public static int initializedObserved = 0;
    public static int beforeDestroyedObserved = 0;

    public static void reset() {
        initializedObserved = 0;
        beforeDestroyedObserved = 0;
    }

    public void observeContextInit(@Observes @Initialized(RequestScoped.class) Object event) {
        initializedObserved++;
    }

    public void observeContextBeforeDestroyed(@Observes @BeforeDestroyed(RequestScoped.class) Object event) {
        beforeDestroyedObserved++;
    }
}
