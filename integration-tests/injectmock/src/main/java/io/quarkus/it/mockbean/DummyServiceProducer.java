package io.quarkus.it.mockbean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

public class DummyServiceProducer {

    @Produces
    @ApplicationScoped
    @Named("first")
    public DummyService dummyService() {
        return new DummyService1();
    }
}
