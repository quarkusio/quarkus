package io.quarkus.observation.cdi;

import jakarta.interceptor.InvocationContext;

import io.micrometer.observation.Observation;

public class ObservedInterceptorContext extends Observation.Context {

    private final InvocationContext invocationContext;

    public ObservedInterceptorContext(InvocationContext invocationContext) {
        this.invocationContext = invocationContext;
    }

    public InvocationContext getInvocationContext() {
        return invocationContext;
    }
}
