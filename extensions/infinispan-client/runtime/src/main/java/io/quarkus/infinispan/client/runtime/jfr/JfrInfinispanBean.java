package io.quarkus.infinispan.client.runtime.jfr;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.infinispan.client.runtime.jfr.event.*;
import io.quarkus.jfr.api.config.JfrRuntimeConfig;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jdk.jfr.FlightRecorder;

@ApplicationScoped
public class JfrInfinispanBean {

    @Inject
    JfrRuntimeConfig jfrRuntimeConfig;

    public void enable() {
        FlightRecorder.register(SingleEntryPeriodEvent.class);
        FlightRecorder.register(SingleEntryStartEvent.class);
        FlightRecorder.register(SingleEntryEndEvent.class);
        FlightRecorder.register(MultiEntryPeriodEvent.class);
        FlightRecorder.register(MultiEntryStartEvent.class);
        FlightRecorder.register(MultiEntryEndEvent.class);
        FlightRecorder.register(CacheWidePeriodEvent.class);
        FlightRecorder.register(CacheWideStartEvent.class);
        FlightRecorder.register(CacheWideEndEvent.class);
    }

    public void disable() {
        FlightRecorder.unregister(SingleEntryPeriodEvent.class);
        FlightRecorder.unregister(SingleEntryStartEvent.class);
        FlightRecorder.unregister(SingleEntryEndEvent.class);
        FlightRecorder.unregister(MultiEntryPeriodEvent.class);
        FlightRecorder.unregister(MultiEntryStartEvent.class);
        FlightRecorder.unregister(MultiEntryEndEvent.class);
        FlightRecorder.unregister(CacheWidePeriodEvent.class);
        FlightRecorder.unregister(CacheWideStartEvent.class);
        FlightRecorder.unregister(CacheWideEndEvent.class);
    }

    public void onStart(@Observes StartupEvent event) {
        if (jfrRuntimeConfig.infinispanEnabled()) {
            enable();
        }
    }

    public void onStop(@Observes ShutdownEvent event) {
        if (jfrRuntimeConfig.infinispanEnabled()) {
            disable();
        }
    }
}
