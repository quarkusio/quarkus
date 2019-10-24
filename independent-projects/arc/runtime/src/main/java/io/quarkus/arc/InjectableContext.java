package io.quarkus.arc;

import java.util.Map;
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
