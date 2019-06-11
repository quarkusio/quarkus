package io.quarkus.arc;

/**
 * Represents an intercepted subclass.
 *
 * @author Martin Kouba
 */
public interface Subclass {

    default void destroy() {
        // Noop
    }

}
