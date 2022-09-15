package io.quarkus.arc;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import java.util.Map;

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
