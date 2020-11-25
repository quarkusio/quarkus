package org.jboss.resteasy.reactive.server.injection;

public interface QuarkusRestInjectionTarget {
    public default void __quarkus_rest_inject(QuarkusRestInjectionContext ctx) {
    }
}
