package org.jboss.protean.arc;

import java.util.Map;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * It can be used by synthetic {@link InjectableBean} definitions to produce a contextual instance.
 *
 * @param <T>
 * @see Contextual#create(CreationalContext)
 */
public interface BeanCreator<T> {

    /**
     *
     * @param creationalContext
     * @param params
     * @return the contextual instance
     */
    T create(CreationalContext<T> creationalContext, Map<String, Object> params);

}