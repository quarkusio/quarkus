package io.quarkus.smallrye.health.runtime.providedchecks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.health.Liveness;

import io.smallrye.health.checks.HeapMemoryHealthCheck;

@Singleton
public class HeapMemoryHealthCheckProducer {

    private Double maxPercentage;

    public HeapMemoryHealthCheckProducer(ProvidedHealthChecksSupport support) {
        this.maxPercentage = support.getHeapMemoryMaxPercentage();
    }

    @Produces
    @ApplicationScoped
    @Liveness
    HeapMemoryHealthCheck produce() {
        if (maxPercentage != null) {
            return new HeapMemoryHealthCheck(maxPercentage);
        } else {
            return new HeapMemoryHealthCheck();
        }
    }

}
