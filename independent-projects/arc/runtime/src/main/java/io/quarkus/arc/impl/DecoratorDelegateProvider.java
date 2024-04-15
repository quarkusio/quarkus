package io.quarkus.arc.impl;

import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.InjectableReferenceProvider;

public class DecoratorDelegateProvider implements InjectableReferenceProvider<Object> {

    @Override
    public Object get(CreationalContext<Object> creationalContext) {
        return getCurrent(creationalContext);
    }

    public static Object getCurrent(CreationalContext<?> ctx) {
        return CreationalContextImpl.getCurrentDecoratorDelegate(ctx);
    }

    /**
     * Set the current delegate for a non-null parameter, or remove it for null parameter.
     *
     * @return the previous delegate or {@code null}
     */
    public static Object setCurrent(CreationalContext<?> ctx, Object delegate) {
        // it wouldn't be necessary to reset this, but we do that as a safeguard,
        // to prevent accidental references from keeping these objects alive
        return CreationalContextImpl.setCurrentDecoratorDelegate(ctx, delegate);
    }

}
