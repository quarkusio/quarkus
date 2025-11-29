package io.quarkus.arc.test.supplement;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConsumerOfSomeBeanInExternalLibrary {
    @Inject
    SomeBeanInExternalLibrary bean;

    public String ping() {
        return bean.ping();
    }
}
