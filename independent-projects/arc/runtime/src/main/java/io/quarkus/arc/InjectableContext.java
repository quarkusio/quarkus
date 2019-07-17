package io.quarkus.arc;

import java.util.Map;
import javax.enterprise.context.spi.AlterableContext;

/**
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
         * The changes to the map are not reflected in the underlying context state.
         * 
         * @return a map of contextual instances
         */
        Map<InjectableBean<?>, Object> getContextualInstances();

    }
}
