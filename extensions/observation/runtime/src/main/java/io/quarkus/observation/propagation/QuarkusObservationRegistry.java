package io.quarkus.observation.propagation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

/**
 * An {@link ObservationRegistry} that stores the current observation scope through
 * {@link ObservationContextStorage}, i.e. in the Vert.x duplicated context when one is available and in a
 * ThreadLocal otherwise.
 * <p>
 * The stock {@link ObservationRegistry#create() SimpleObservationRegistry} keeps the current scope in a plain
 * ThreadLocal, so a scope opened while serving a request would not survive a dispatch to another thread within
 * the same request. By routing the scope accessors ({@link #getCurrentObservationScope()},
 * {@link #setCurrentObservationScope(Observation.Scope)} and {@link #getCurrentObservation()}) through
 * {@code ObservationContextStorage}, the scope is bound to the duplicated context and therefore propagates the
 * same way OpenTelemetry contexts do (see {@code QuarkusContextStorage}).
 * <p>
 * Everything else (handlers, predicates, filters, conventions) is delegated to a standard registry.
 */
public final class QuarkusObservationRegistry implements ObservationRegistry {

    private final ObservationRegistry delegate;

    public QuarkusObservationRegistry() {
        this.delegate = ObservationRegistry.create();
    }

    @Override
    public Observation getCurrentObservation() {
        Observation.Scope scope = ObservationContextStorage.currentScope();
        return scope != null ? scope.getCurrentObservation() : null;
    }

    @Override
    public Observation.Scope getCurrentObservationScope() {
        return ObservationContextStorage.currentScope();
    }

    @Override
    public void setCurrentObservationScope(Observation.Scope current) {
        ObservationContextStorage.setCurrentScope(current);
    }

    @Override
    public ObservationConfig observationConfig() {
        return delegate.observationConfig();
    }

    @Override
    public boolean isNoop() {
        return delegate.isNoop();
    }
}
