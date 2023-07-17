package io.quarkus.arc.impl;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.InjectableBean;

/**
 *
 * @param <T>
 */
class LazyInstanceHandle<T> extends AbstractInstanceHandle<T> {

    LazyInstanceHandle(InjectableBean<T> bean, CreationalContext<T> creationalContext,
            CreationalContext<?> parentCreationalContext, Supplier<T> createLogic,
            Consumer<T> destroyLogic) {
        super(bean, creationalContext, parentCreationalContext, destroyLogic);
        this.value = new LazyValue<>(createLogic);
    }

    private final LazyValue<T> value;

    @Override
    protected boolean isInstanceCreated() {
        return value.isSet();
    }

    @Override
    protected T instanceInternal() {
        return value.get();
    }

}
