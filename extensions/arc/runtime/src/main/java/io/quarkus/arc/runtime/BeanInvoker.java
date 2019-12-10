package io.quarkus.arc.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;

/**
 * Invokes a business method of a bean. The request context is activated if necessary.
 * 
 * @param <T>
 */
public interface BeanInvoker<T> {

    default void invoke(T param) {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            invokeBean(param);
        } else {
            try {
                requestContext.activate();
                invokeBean(param);
            } finally {
                requestContext.terminate();
            }
        }
    }

    void invokeBean(T param);

}
