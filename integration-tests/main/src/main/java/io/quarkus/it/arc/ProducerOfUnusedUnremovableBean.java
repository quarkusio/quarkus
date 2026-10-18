package io.quarkus.it.arc;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.Unremovable;

public class ProducerOfUnusedUnremovableBean {

    @Unremovable
    @Produces
    public Bean produce() {
        return new Bean();
    }

    @Dependent
    public static class Bean {

    }
}
