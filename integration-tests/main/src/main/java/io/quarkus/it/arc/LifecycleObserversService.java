package io.quarkus.it.arc;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class LifecycleObserversService {
    public static final AtomicBoolean STARTUP_METHOD_CALLED = new AtomicBoolean();
    public static final AtomicBoolean STARTUP_EVENT_OBSERVED = new AtomicBoolean();

    @Startup
    void startup() {
        STARTUP_METHOD_CALLED.set(true);
    }

    void onStart(@Observes StartupEvent event) {
        STARTUP_EVENT_OBSERVED.set(true);
    }
}
