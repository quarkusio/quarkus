package org.jboss.protean.arc;

import java.util.Collection;

/**
 *
 * @author Martin Kouba
 */
public interface ManagedContext extends InjectableContext {

    /**
     * Activate the context with no initial state.
     */
    default void activate() {
        activate(null);
    }

    /**
     * Activate the context. All instance handles from the initial state must have the same scope as the context, otherwise an {@link IllegalArgumentException}
     * is thrown.
     *
     * @param initialState The initial state, may be {@code null}
     */
    void activate(Collection<InstanceHandle<?>> initialState);

    /**
     * Deactivate the context - do not destoy existing contextual instances.
     */
    void deactivate();

    /**
     * Destroy and deactivate the context.
     */
    default void terminate() {
        destroy();
        deactivate();
    }
}
