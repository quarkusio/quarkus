package io.quarkus.arc.test.contexts.session;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class ContextObserver {

    static volatile int initializedObserved = 0;
    static volatile int beforeDestroyedObserved = 0;
    static volatile int destroyedObserved = 0;

    static void reset() {
        initializedObserved = 0;
        beforeDestroyedObserved = 0;
        destroyedObserved = 0;
    }

    void observeContextInit(@Observes @Initialized(SessionScoped.class) Object event) {
        initializedObserved++;
    }

    void observeContextBeforeDestroyed(@Observes @BeforeDestroyed(SessionScoped.class) Object event) {
        beforeDestroyedObserved++;
    }

    void observeContextDestroyed(@Observes @Destroyed(SessionScoped.class) Object event) {
        destroyedObserved++;
    }
}
