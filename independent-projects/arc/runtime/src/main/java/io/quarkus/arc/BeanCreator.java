package io.quarkus.arc;

import java.util.Map;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.CreationException;

/**
 * This interface is used by synthetic beans to produce a contextual instance.
 *
 * @param <T>
 * @see InjectableBean
 * @see Contextual#create(CreationalContext)
 */
public interface BeanCreator<T> {

    /**
     *
     * @param context
     * @return the contextual instance
     */
    default T create(SyntheticCreationalContext<T> context) {
        return create(context, context.getParams());
    }

    /**
     *
     * @param creationalContext
     * @param params
     * @return the contextual instance
     * @deprecated Use {@link #create(SyntheticCreationalContext)} instead
     */
    @Deprecated(forRemoval = true, since = "3.0")
    default T create(CreationalContext<T> creationalContext, Map<String, Object> params) {
        throw new CreationException("Creation logic not implemented");
    }

}
