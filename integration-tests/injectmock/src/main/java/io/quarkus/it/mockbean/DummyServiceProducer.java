package io.quarkus.it.mockbean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

public class DummyServiceProducer {

    @Produces
    @ApplicationScoped
    @Named("first")
    public DummyService dummyService() {
        return new DummyService1();
    }
}
