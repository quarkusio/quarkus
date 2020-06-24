package io.quarkus.arc;

import java.util.Map;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.AlterableContext;

/**
 * A context implementing this interface allows to capture and view its state via {@link ContextState}.
 * It also allows user to destroy all contextual instances within this context.
 *
 * @author Martin Kouba
 */
public interface InjectableContext extends AlterableContext {

    /**
     * Destroy all existing contextual instances.
     */
    void destroy();

    /**
     * @return the current state
     */
    ContextState getState();

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

    /**
    *
    */
    interface ContextState {

        /**
         * The changes to the map are not reflected in the underlying context.
         * 
         * @return a map of contextual instances
         */
        Map<InjectableBean<?>, Object> getContextualInstances();

    }
}
