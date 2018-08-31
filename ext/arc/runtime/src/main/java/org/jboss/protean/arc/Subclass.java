package org.jboss.protean.arc;

/**
 *
 * @author Martin Kouba
 */
public interface Subclass {

    default void destroy() {
        // Noop
    }

}
