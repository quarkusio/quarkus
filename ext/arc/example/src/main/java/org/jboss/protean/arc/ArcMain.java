package org.jboss.protean.arc;

import org.jboss.protean.arc.Arc;

public class ArcMain {

    static {
        // This is needed for graal ahead-of-time compilation
        // ArcContainer collects all beans using a service provider
        Arc.initialize();
    }

    public static void main(String[] args) {
        Arc.container().instance(Generator.class).get().run();
        Arc.shutdown();
    }

}
