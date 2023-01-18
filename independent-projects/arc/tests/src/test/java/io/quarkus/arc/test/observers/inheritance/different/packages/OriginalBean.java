package io.quarkus.arc.test.observers.inheritance.different.packages;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class OriginalBean {

    public static int TIMES_INVOKED = 0;

    // package private observer, not visible from AlternativeBean
    void observerEvent(@Observes @Initialized(ApplicationScoped.class) Object event) {
        TIMES_INVOKED++;
    }

    public String ping() {
        return OriginalBean.class.getSimpleName();
    }

}
