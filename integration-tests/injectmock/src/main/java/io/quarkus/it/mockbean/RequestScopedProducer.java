package io.quarkus.it.mockbean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.Unremovable;

public class RequestScopedProducer {

    @Produces
    @RequestScoped
    @Unremovable
    public RequestScopedFooFromProducer produce() {
        return new RequestScopedFooFromProducer();
    }

}
