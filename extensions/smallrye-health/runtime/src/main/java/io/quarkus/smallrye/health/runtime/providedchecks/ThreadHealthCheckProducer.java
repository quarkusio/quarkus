package io.quarkus.smallrye.health.runtime.providedchecks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.health.Liveness;

import io.smallrye.health.checks.ThreadHealthCheck;

@Singleton
public class ThreadHealthCheckProducer {

    private long max;

    public ThreadHealthCheckProducer(ProvidedHealthChecksSupport support) {
        this.max = support.getThreadMax();
    }

    @Produces
    @ApplicationScoped
    @Liveness
    ThreadHealthCheck produce() {
        return new ThreadHealthCheck(max);
    }

}
