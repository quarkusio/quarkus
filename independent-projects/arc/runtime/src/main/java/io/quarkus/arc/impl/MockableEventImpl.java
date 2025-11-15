package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.TypeLiteral;

public class MockableEventImpl<T> extends EventImpl<T> implements Mockable {

    private final AtomicReference<Event<?>> mock;

    MockableEventImpl(Type eventType, Set<Annotation> qualifiers, InjectionPoint injectionPoint,
            AtomicReference<Event<?>> mock) {
        super(eventType, qualifiers, injectionPoint);
        this.mock = mock;
    }

    @Override
    public void fire(T event) {
        @SuppressWarnings("unchecked")
        Event<T> m = (Event<T>) mock.get();
        if (m != null) {
            m.fire(event);
        } else {
            super.fire(event);
        }
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event) {
        @SuppressWarnings("unchecked")
        Event<T> m = (Event<T>) mock.get();
        if (m != null) {
            return m.fireAsync(event);
        } else {
            return super.fireAsync(event);
        }
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event, NotificationOptions options) {
        @SuppressWarnings("unchecked")
        Event<T> m = (Event<T>) mock.get();
        if (m != null) {
            return m.fireAsync(event, options);
        } else {
            return super.fireAsync(event, options);
        }
    }

    @Override
    public Event<T> select(Annotation... qualifiers) {
        @SuppressWarnings("unchecked")
        Event<T> m = (Event<T>) mock.get();
        if (m != null) {
            return m.select(qualifiers);
        } else {
            return super.select(qualifiers);
        }
    }

    @Override
    public <U extends T> Event<U> select(Class<U> subtype, Annotation... qualifiers) {
        @SuppressWarnings("unchecked")
        Event<T> m = (Event<T>) mock.get();
        if (m != null) {
            return m.select(subtype, qualifiers);
        } else {
            return super.select(subtype, qualifiers);
        }
    }

    @Override
    public <U extends T> Event<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        @SuppressWarnings("unchecked")
        Event<T> m = (Event<T>) mock.get();
        if (m != null) {
            return m.select(subtype, qualifiers);
        } else {
            return super.select(subtype, qualifiers);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void arc$setMock(Object instance) {
        this.mock.set((Event<T>) instance);
    }

    @Override
    public void arc$clearMock() {
        this.mock.set(null);
    }

}
