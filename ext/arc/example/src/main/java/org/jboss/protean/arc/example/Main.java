package org.jboss.protean.arc.example;

import org.jboss.protean.arc.Arc;

/**
 *
 * @author Martin Kouba
 */
public class Main {

    static {
        // This is needed for graal ahead-of-time compilation
        // ArcContainer collects all beans using a service provider
        Arc.initialize();
    }

    public static void main(String[] args) {
        Baz baz = Arc.container().instance(Baz.class).get();
        System.out.println(baz.pingFoo());
    }

}
