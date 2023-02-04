package io.quarkus.it.metrics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.AroundConstruct;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * The purpose of this bean is to test that ArC automatically registers the constructor for reflection if an
 * {@link AroundConstruct} interceptors is used. We use a dedicated bean to make sure the class is not registered by another
 * extension, ie. by resteasy in case of JAX-RS resource.
 *
 * See https://github.com/quarkusio/quarkus/issues/6898
 */
@ApplicationScoped
@RegisterForReflection(fields = false)
public class DummyGauge {

    void init(@Observes StartupEvent event) {
    }

    @Gauge(name = "dummyGauge", unit = MetricUnits.NONE)
    public Long dummyGauge() {
        return 42l;
    }

}
