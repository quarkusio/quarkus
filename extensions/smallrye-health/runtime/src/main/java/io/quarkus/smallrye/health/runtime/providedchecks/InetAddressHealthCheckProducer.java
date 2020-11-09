package io.quarkus.smallrye.health.runtime.providedchecks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.health.Liveness;

import io.smallrye.health.checks.InetAddressHealthCheck;

@Singleton
public class InetAddressHealthCheckProducer {

    private String host;

    public InetAddressHealthCheckProducer(ProvidedHealthChecksSupport support) {
        this.host = support.getInetAddressHost();
    }

    @Produces
    @ApplicationScoped
    @Liveness
    InetAddressHealthCheck produce() {
        return new InetAddressHealthCheck(host);
    }

}
