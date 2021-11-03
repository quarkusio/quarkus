package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.enterprise.context.spi.CreationalContext;

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
