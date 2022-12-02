package io.quarkus.arc.test.alternatives;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;

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