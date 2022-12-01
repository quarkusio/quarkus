package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;

import io.quarkus.arc.CurrentContext;
import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.arc.InjectableContext.ContextState;

/**
 * The default implementation makes use of {@link ThreadLocal} variables.
 *
 * @see ThreadLocalCurrentContext
 */
final class ThreadLocalCurrentContextFactory implements CurrentContextFactory {

    @Override
    public <T extends ContextState> CurrentContext<T> create(Class<? extends Annotation> scope) {
        return new ThreadLocalCurrentContext<>();
    }

}
