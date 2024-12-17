package io.quarkus.websockets.next.runtime;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Any;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.arc.impl.ComputingCacheContextInstances;
import io.quarkus.arc.impl.CurrentManagedContext;
import io.quarkus.arc.impl.LazyValue;

public class WebSocketSessionContext extends CurrentManagedContext {

    public WebSocketSessionContext(CurrentContextFactory currentContextFactory) {
        super(currentContextFactory.create(SessionScoped.class), ComputingCacheContextInstances::new,
                newEvent(Initialized.Literal.SESSION, Any.Literal.INSTANCE),
                newEvent(BeforeDestroyed.Literal.SESSION, Any.Literal.INSTANCE),
                newEvent(Destroyed.Literal.SESSION, Any.Literal.INSTANCE));
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return SessionScoped.class;
    }

    protected ContextNotActiveException notActive() {
        return new ContextNotActiveException("Session context is not active");
    }

    private static Consumer<Object> newEvent(Annotation... qualifiers) {
        LazyValue<Event<Object>> event = new LazyValue<>(new Supplier<Event<Object>>() {
            @Override
            public Event<Object> get() {
                ArcContainer container = Arc.container();
                if (container.resolveObserverMethods(Object.class, qualifiers).isEmpty()) {
                    return null;
                }
                return container.beanManager().getEvent().select(qualifiers);
            }
        });
        return new Consumer<Object>() {

            @Override
            public void accept(Object t) {
                Event<Object> e = event.get();
                if (e != null) {
                    e.fire(t);
                }
            }
        };
    }
}
