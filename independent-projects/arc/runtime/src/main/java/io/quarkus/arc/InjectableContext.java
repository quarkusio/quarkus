package io.quarkus.arc;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import java.util.Map;
import java.util.function.Function;

/**
 * A context implementing this interface makes it possible to capture and view its state via the {@link ContextState}.
 *
 * It also allows users to destroy all contextual instances within this context.
 */
public interface InjectableContext extends AlterableContext {

    /**
     * Destroy all existing contextual instances.
     */
    void destroy();

    /**
     * @return the current state
     * @throws ContextNotActiveException
     */
    ContextState getState();

    /**
     * If the context is active then return the current state.
     *
     * @return the current state or {@code null} if the context is not active
     */
    default ContextState getStateIfActive() {
        return isActive() ? getState() : null;
    }

    /**
     * If the context is active then return an existing instance of certain contextual type or create a new instance, otherwise
     * return a null value.
     *
     * This allows for the {@link #isActive()} check and the actual creation to happen in a single method, which gives a
     * performance benefit by performing fewer thread local operations.
     *
     * @param <T> the type of contextual type
     * @param contextual the contextual type
     * @param creationalContextFunction the creational context function
     * @return the contextual instance, or a null value
     */
    default <T> T getIfActive(Contextual<T> contextual,
            Function<Contextual<T>, CreationalContext<T>> creationalContextFunction) {
        if (!isActive()) {
            return null;
        }
        T result = get(contextual);
        if (result != null) {
            return result;
        }
        return get(contextual, creationalContextFunction.apply(contextual));
    }

    /**
     * Destroy all contextual instances from the given state.
     * <p>
     * The default implementation is not optimized and does not guarantee proper sychronization. Implementations of this
     * interface are encouraged to provide an optimized implementation of this method.
     *
     * @param state
     */
    default void destroy(ContextState state) {
        for (InjectableBean<?> bean : state.getContextualInstances().keySet()) {
            try {
                destroy(bean);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to destroy contextual instance of " + bean, e);
            }
        }
    }

    /**
     *
     * @return {@code true} if this context represents a normal scope
     */
    default boolean isNormal() {
        return getScope().isAnnotationPresent(NormalScope.class);
    }

    interface ContextState {

        /**
         * @return an immutable map of contextual instances
         */
        Map<InjectableBean<?>, Object> getContextualInstances();

        /**
         * Context state is typically invalidated once the context to which is belongs is being destroyed.
         * This flag is then used by context propagation to indicate that the given state shouldn't be reused anymore.
         *
         * @return true if the context state is valid, false otherwise
         */
        default boolean isValid() {
            return true;
        }

    }
}
