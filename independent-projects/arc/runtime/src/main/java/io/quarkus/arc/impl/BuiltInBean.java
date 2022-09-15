package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean;
import jakarta.enterprise.context.spi.CreationalContext;

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

    @Override
    public Kind getKind() {
        return Kind.BUILTIN;
    }

    @Override
    public String toString() {
        return Beans.toString(this);
    }

}
