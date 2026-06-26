package io.quarkus.opentelemetry.observation.propagation;

import io.micrometer.observation.Observation;

class PropagatedObservationScope implements Observation.Scope {

    private final Observation observation;
    private final Observation.Scope previousScope;

    PropagatedObservationScope(Observation observation, Observation.Scope previousScope) {
        this.observation = observation;
        this.previousScope = previousScope;
    }

    @Override
    public Observation getCurrentObservation() {
        return observation;
    }

    @Override
    public Observation.Scope getPreviousObservationScope() {
        return previousScope;
    }

    @Override
    public void makeCurrent() {
        // No-op — scope is managed by the context propagation provider
    }

    @Override
    public void reset() {
        // No-op — scope is managed by the context propagation provider
    }

    @Override
    public void close() {
        // Managed by endContext(), not by close()
    }
}
