package io.quarkus.rest.client.reactive;

import java.util.List;

/**
 * Allows methods that are meant to compute a value for a Rest Client method to have access to the
 * context on which they are invoked
 */
public interface ComputedParamContext {
    /**
     * The name of the parameter whose value is being computed
     */
    String name();

    /**
     * Information about the method parameters of the REST Client method for which the computed value is needed
     */
    List<MethodParameter> methodParameters();

    interface MethodParameter {
        Object value();
    }
}
