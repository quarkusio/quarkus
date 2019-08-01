package io.quarkus.arc;

import javax.enterprise.context.spi.CreationalContext;

/**
 * Common class for all built-in beans.
 *
 */
public abstract class BuiltInBean<T> implements InjectableBean<T> {

    @Override
    public String getIdentifier() {
        return "builtin_bean_" + this.getClass().getSimpleName();
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        return get(creationalContext);
    }
}
