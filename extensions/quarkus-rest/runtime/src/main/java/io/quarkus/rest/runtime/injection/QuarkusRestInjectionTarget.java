package io.quarkus.rest.runtime.injection;

public interface QuarkusRestInjectionTarget {
    public default void __quarkus_rest_inject(QuarkusRestInjectionContext ctx) {
    }
}
