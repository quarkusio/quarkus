package org.jboss.protean.arc;

import java.util.Map;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * It can be used by synthetic {@link InjectableBean} definitions to destroy a contextual instance.
 *
 * @param <T>
 * @see Contextual#destroy(Object, CreationalContext)
 */
public interface BeanDestroyer<T> {

    /**
     *
     * @param instance
     * @param creationalContext
     * @param params
     */
    void destroy(T instance, CreationalContext<T> creationalContext, Map<String, Object> params);

}