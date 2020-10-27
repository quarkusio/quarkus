package io.quarkus.rest.server.runtime.injection;

public interface QuarkusRestInjectionTarget {
    public default void __quarkus_rest_inject(QuarkusRestInjectionContext ctx) {
    }
}
