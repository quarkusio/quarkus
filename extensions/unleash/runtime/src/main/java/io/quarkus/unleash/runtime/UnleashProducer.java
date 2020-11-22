package io.quarkus.unleash.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import no.finn.unleash.Unleash;

@ApplicationScoped
public class UnleashProducer {

    private Unleash unleash;

    void initialize(UnleashRuntimeTimeConfig config) {
        unleash = new UnleashCreator(config).createUnleash();
    }

    @Produces
    @DefaultBean
    public Unleash createUnleash() {
        return unleash;
    }

    @PreDestroy
    public void destroy() {
        if (unleash != null) {
            unleash.shutdown();
        }
    }
}
