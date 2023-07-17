package io.quarkus.arc.test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class SimpleBean {

    static final String DEFAULT = "bar";

    private final AtomicReference<StartupEvent> startupEvent = new AtomicReference<StartupEvent>();

    @Inject
    @ConfigProperty(name = "unconfigured", defaultValue = DEFAULT)
    String foo;

    @Inject
    @ConfigProperty(name = "unconfigured")
    Optional<String> fooOptional;

    @Inject
    @ConfigProperty(name = "simpleBean.baz")
    Optional<String> bazOptional;

    @Inject
    @ConfigProperty(name = "simpleBean.baz")
    Provider<String> bazProvider;

    void onStart(@Observes StartupEvent event) {
        startupEvent.set(event);
    }

    AtomicReference<StartupEvent> getStartupEvent() {
        return startupEvent;
    }

    String getFoo() {
        return foo;
    }

    Optional<String> getFooOptional() {
        return fooOptional;
    }

    Optional<String> getBazOptional() {
        return bazOptional;
    }

    public Provider<String> getBazProvider() {
        return bazProvider;
    }

}
