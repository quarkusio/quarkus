package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.SessionScoped;

import io.quarkus.arc.CurrentContext;
import io.quarkus.arc.impl.EventImpl.Notifier;

/**
 * The built-in context for {@link SessionScoped}.
 */
public class SessionContext extends CurrentManagedContext {

    public SessionContext(CurrentContext<CurrentContextState> currentContext, Notifier<Object> initializedNotifier,
            Notifier<Object> beforeDestroyedNotifier, Notifier<Object> destroyedNotifier,
            Supplier<ContextInstances> contextInstances) {
        super(currentContext, contextInstances, initializedNotifier != null ? initializedNotifier::notify : null,
                beforeDestroyedNotifier != null ? beforeDestroyedNotifier::notify : null,
                destroyedNotifier != null ? destroyedNotifier::notify : null);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return SessionScoped.class;
    }

    @Override
    protected ContextNotActiveException notActive() {
        return new ContextNotActiveException("Session context is not active");
    }

}
