package io.quarkus.smallrye.health.runtime.providedchecks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.health.Liveness;

import io.smallrye.health.checks.NonHeapMemoryHealthCheck;

@Singleton
public class NonHeapMemoryHealthCheckProducer {

    private Double maxPercentage;

    public NonHeapMemoryHealthCheckProducer(ProvidedHealthChecksSupport support) {
        this.maxPercentage = support.getNonHeapMemoryMaxPercentage();
    }

    @Produces
    @ApplicationScoped
    @Liveness
    NonHeapMemoryHealthCheck produce() {
        if (maxPercentage != null) {
            return new NonHeapMemoryHealthCheck(maxPercentage);
        } else {
            return new NonHeapMemoryHealthCheck();
        }
    }

}
