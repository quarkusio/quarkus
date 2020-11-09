package io.quarkus.smallrye.health.runtime.providedchecks;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.health.Liveness;

import io.smallrye.health.checks.UrlHealthCheck;

@Singleton
public class UrlHealthCheckProducer {

    private String url;

    public UrlHealthCheckProducer(ProvidedHealthChecksSupport support) {
        this.url = support.getUrlAddress();
    }

    @Produces
    @ApplicationScoped
    @Liveness
    UrlHealthCheck produce() {
        return new UrlHealthCheck(url);
    }

}
