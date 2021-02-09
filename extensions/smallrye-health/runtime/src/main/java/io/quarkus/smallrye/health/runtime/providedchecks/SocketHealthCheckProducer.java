package io.quarkus.smallrye.health.runtime.providedchecks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.health.Liveness;

import io.smallrye.health.checks.SocketHealthCheck;

@Singleton
public class SocketHealthCheckProducer {

    private String host;

    private Integer port;

    public SocketHealthCheckProducer(ProvidedHealthChecksSupport support) {
        this.host = support.getSocketHost();
        this.port = support.getSocketPort();
    }

    @Produces
    @ApplicationScoped
    @Liveness
    SocketHealthCheck produce() {
        return new SocketHealthCheck(host, port);
    }

}
