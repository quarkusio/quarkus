package io.quarkus.smallrye.health.runtime.providedchecks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.health.Liveness;

import io.smallrye.health.checks.SystemLoadHealthCheck;

@Singleton
public class SystemLoadHealthCheckProducer {

    private Double max;

    public SystemLoadHealthCheckProducer(ProvidedHealthChecksSupport support) {
        this.max = support.getSystemLoadMax();
    }

    @Produces
    @ApplicationScoped
    @Liveness
    SystemLoadHealthCheck produce() {
        return new SystemLoadHealthCheck(max);
    }

}
