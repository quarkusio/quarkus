package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableReferenceProvider;
import jakarta.enterprise.context.spi.CreationalContext;

public class DecoratorDelegateProvider implements InjectableReferenceProvider<Object> {

    private static final ThreadLocal<Object> CURRENT = new ThreadLocal<>();

    @Override
    public Object get(CreationalContext<Object> creationalContext) {
        return CURRENT.get();
    }

    /**
     * Set the current delegate for a non-null parameter, remove the threadlocal for null parameter.
     *
     * @param delegate
     * @return the previous delegate or {@code null}
     */
    public static Object set(Object delegate) {
        if (delegate != null) {
            Object prev = CURRENT.get();
            if (delegate.equals(prev)) {
                return delegate;
            } else {
                CURRENT.set(delegate);
                return prev;
            }
        } else {
            CURRENT.remove();
            return null;
        }
    }

    public static void unset() {
        set(null);
    }

    public static Object get() {
        return CURRENT.get();
    }

}
