package io.quarkus.it.metrics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;

import io.quarkus.runtime.StartupEvent;

/**
 * The purpose of this bean is to test that a Gauge works in native mode even if its declaring bean is
 * not explicitly registered for reflection.
 */
@ApplicationScoped
public class DummyGaugeNoReflectionAnnotation {

    void init(@Observes StartupEvent event) {
    }

    @Gauge(name = "dummyGauge", unit = MetricUnits.NONE)
    public Long dummyGauge() {
        return 1234l;
    }

}
