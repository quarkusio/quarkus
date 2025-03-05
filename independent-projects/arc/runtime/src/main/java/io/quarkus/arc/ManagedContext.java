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
     * <p>
     * If needed, activating a context will fire {@code @Initialized} event for the given context.
     *
     * @return the context state
     */
    default ContextState activate() {
        return activate(null);
    }

    // Maintain binary compatibility with Quarkus 3.2
    default void activate$$bridge() {
        activate(null);
    }

    /**
     * Activate the context.
     * <p>
     * If invoked with {@code null} parameter, a fresh {@link io.quarkus.arc.InjectableContext.ContextState} is
     * automatically created.
     *
     * @param initialState The initial state, may be {@code null}
     * @return the context state
     */
    ContextState activate(ContextState initialState);

    // Maintain binary compatibility with Quarkus 3.2
    default void activate$$bridge(ContextState initialState) {
        activate(initialState);
    }

    /**
     * Deactivate the context - do not destroy existing contextual instances.
     */
    void deactivate();

    /**
     * Destroy and deactivate the context.
     */
    default void terminate() {
        destroy();
        deactivate();
    }

    /**
     * Creates a new {@link io.quarkus.arc.InjectableContext.ContextState}.
     * <p>
     * Creating a context state does not fire {@code @Initialized} event for given context.
     *
     * @return a new initialized context state
     */
    ContextState initializeState();
}
