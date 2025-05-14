package io.quarkus.undertow.test.sessioncontext;

import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Observes;

@SessionScoped
public class SessionScopedObserver {

    static int timesInitObserved = 0;
    static int timesBeforeDestroyedObserved = 0;

    public void observeInit(@Observes @Initialized(SessionScoped.class) Object event) {
        timesInitObserved++;
    }

    public void observeBeforeDestroyed(@Observes @BeforeDestroyed(SessionScoped.class) Object event) {
        timesBeforeDestroyedObserved++;
    }

    public static void resetState() {
        timesInitObserved = 0;
        timesBeforeDestroyedObserved = 0;
    }
}
