package io.quarkus.arc.impl;

import io.quarkus.arc.CurrentContext;
import io.quarkus.arc.InjectableContext.ContextState;

/**
 * {@link ThreadLocal} implementation of {@link CurrentContext}.
 *
 * @param <T>
 */
final class ThreadLocalCurrentContext<T extends ContextState> implements CurrentContext<T> {

    private final ThreadLocal<T> currentContext = new ThreadLocal<>();

    @Override
    public T get() {
        return currentContext.get();
    }

    @Override
    public void set(T state) {
        currentContext.set(state);
    }

    @Override
    public void remove() {
        currentContext.remove();
    }

}
