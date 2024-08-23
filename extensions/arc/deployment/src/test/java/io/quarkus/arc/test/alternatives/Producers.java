package io.quarkus.arc.test.alternatives;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
class Producers {

    @Alternative
    @Produces
    static final int CHARLIE = 10;

    @Produces
    @Alternative
    public String bravo() {
        return "bravo";
    }
}
