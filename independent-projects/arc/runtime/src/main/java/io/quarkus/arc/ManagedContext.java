package io.quarkus.arc;

/**
 * A context implementing this interface can be manually managed.
 * It can be activated with certain state hence allowing for context propagation and then deactivated on demand.
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
     * If invoked with {@code null} parameter, a fresh {@link io.quarkus.arc.InjectableContext.ContextState} is
     * automatically created.
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
