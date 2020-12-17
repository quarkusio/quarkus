package io.quarkus.resteasy.reactive.links.runtime;

/**
 * An accessor that knows how to access a specific getter method of a specific type.
 */
public interface GetterAccessor {

    /**
     * Access a getter on a given instance and return a response.
     */
    Object get(Object instance);
}
