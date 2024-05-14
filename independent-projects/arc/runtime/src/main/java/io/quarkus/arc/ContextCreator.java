package io.quarkus.arc;

import java.util.Map;

/**
 * Can be used to create an {@link InjectableContext} instance.
 */
public interface ContextCreator {

    /**
     * This key can be used to obtain the {@link CurrentContextFactory} from the map of parameters.
     */
    String KEY_CURRENT_CONTEXT_FACTORY = "io.quarkus.arc.currentContextFactory";

    /**
     *
     * @param params
     * @return the context instance
     * @see ContextCreator#KEY_CURRENT_CONTEXT_FACTORY
     */
    InjectableContext create(Map<String, Object> params);

}