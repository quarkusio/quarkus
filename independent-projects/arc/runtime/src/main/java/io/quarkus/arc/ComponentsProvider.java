package io.quarkus.arc;

/**
 * Service provider interface used to colllect the runtime components.
 */
public interface ComponentsProvider {

    Components getComponents();

}