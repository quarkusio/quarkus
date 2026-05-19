package io.quarkus.aesh.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.aesh.AeshRuntimeRunner;

/**
 * Produces CDI beans for Aesh components that users can inject into their applications.
 */
@ApplicationScoped
public class AeshProducer {

    /**
     * Produces an AeshRuntimeRunner for runtime mode.
     * This uses the AeshRuntimeRunnerFactory to create the runner with the top command.
     *
     * @param factory the factory to create the runner (if available)
     * @return an AeshRuntimeRunner configured with the top command
     */
    @Produces
    @Singleton
    public AeshRuntimeRunner aeshRuntimeRunner(Instance<AeshRuntimeRunnerFactory> factory) {
        if (factory.isResolvable()) {
            return factory.get().create();
        }
        throw new IllegalStateException(
                "No AeshRuntimeRunnerFactory available. Make sure you have a command annotated with @TopCommand.");
    }
}
