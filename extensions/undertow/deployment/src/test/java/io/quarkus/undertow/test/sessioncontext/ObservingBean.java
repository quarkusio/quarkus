package io.quarkus.undertow.test.sessioncontext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class ObservingBean {

    int timesInitObserved = 0;
    int timesBeforeDestroyedObserved = 0;
    int timesDestroyedObserved = 0;

    public int getTimesInitObserved() {
        return timesInitObserved;
    }

    public int getTimesBeforeDestroyedObserved() {
        return timesBeforeDestroyedObserved;
    }

    public int getTimesDestroyedObserved() {
        return timesDestroyedObserved;
    }

    public void observeInit(@Observes @Initialized(SessionScoped.class) Object event) {
        timesInitObserved++;
    }

    public void observeBeforeDestroyed(@Observes @BeforeDestroyed(SessionScoped.class) Object event) {
        timesBeforeDestroyedObserved++;
    }

    public void observeDestroyed(@Observes @Destroyed(SessionScoped.class) Object event) {
        timesDestroyedObserved++;
    }

    public void resetState() {
        this.timesInitObserved = 0;
        this.timesBeforeDestroyedObserved = 0;
        this.timesDestroyedObserved = 0;
    }
}
