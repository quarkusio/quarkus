package io.quarkus.arc.test.supplement;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class SomeBeanInExternalLibrary implements SomeInterfaceInExternalLibrary {
    public static boolean pinged;
    public static boolean observed;
    public static boolean produced;
    public static boolean disposed;

    @Inject
    SomeDependencyInExternalLibrary dependency;

    @Override
    public String hello() {
        return dependency.hello();
    }

    // methods below are intentionally package-private to verify
    // behavior in Quarkus dev mode (multiple classloaders)

    String ping() {
        pinged = true;
        return "pong";
    }

    void init(@Observes SomeEventInExternalLibrary event) {
        observed = true;
    }

    @Produces
    @Dependent
    SomeProducedDependencyInExternalLibrary produce(SomeDependencyInExternalLibrary dependency) {
        produced = true;
        return new SomeProducedDependencyInExternalLibrary(dependency);
    }

    void dispose(@Disposes SomeProducedDependencyInExternalLibrary dependency) {
        disposed = true;
    }
}
