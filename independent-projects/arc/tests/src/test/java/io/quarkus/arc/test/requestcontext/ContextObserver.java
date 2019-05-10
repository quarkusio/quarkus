package io.quarkus.arc.test.requestcontext;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;

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
