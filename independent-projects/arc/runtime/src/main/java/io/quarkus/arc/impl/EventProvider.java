package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.InjectionPoint;

import io.quarkus.arc.InjectableReferenceProvider;

/**
 *
 * @author Martin Kouba
 */
public class EventProvider<T> implements InjectableReferenceProvider<Event<T>> {

    private final Type eventType;
    private final Set<Annotation> eventQualifiers;
    private final InjectionPoint injectionPoint;

    public EventProvider(Type eventType, Set<Annotation> eventQualifiers, InjectionPoint injectionPoint) {
        this.eventType = eventType;
        this.eventQualifiers = eventQualifiers;
        this.injectionPoint = injectionPoint;
    }

    @Override
    public Event<T> get(CreationalContext<Event<T>> creationalContext) {
        return new EventImpl<>(eventType, eventQualifiers, injectionPoint);
    }

}
