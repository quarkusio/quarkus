package io.quarkus.arc.test.supplement;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class ConsumerOfSomeDepBeanInExternalLibrary {
    @Inject
    SomeDepBeanInExternalLibrary bean;

    public String ping() {
        return bean.ping();
    }
}
