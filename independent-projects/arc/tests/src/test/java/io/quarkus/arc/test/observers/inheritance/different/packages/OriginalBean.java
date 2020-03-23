package io.quarkus.arc.test.observers.inheritance.different.packages;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

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
