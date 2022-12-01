package org.jboss.resteasy.reactive.server.injection;

public interface ResteasyReactiveInjectionTarget {

    default void __quarkus_rest_inject(ResteasyReactiveInjectionContext ctx) {
    }
}
