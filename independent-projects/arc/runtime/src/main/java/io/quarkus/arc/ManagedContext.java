package io.quarkus.arc;

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
     * Activate the context.
     * 
     * @param initialState The initial state, may be {@code null}
     */
    void activate(ContextState initialState);

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
