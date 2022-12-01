package io.quarkus.arc;

import java.util.Map;

/**
 * Can be used to create an {@link InjectableContext} instance.
 */
public interface ContextCreator {

    /**
     *
     * @param params
     * @return the context instance
     */
    InjectableContext create(Map<String, Object> params);

}