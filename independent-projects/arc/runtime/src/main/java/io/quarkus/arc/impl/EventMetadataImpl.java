package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.enterprise.inject.spi.InjectionPoint;

// this class is public because it is used in io.quarkus.arc.InjectableObserverMethod
public final class EventMetadataImpl implements EventMetadata {

    private final Set<Annotation> qualifiers;
    private final Type eventType;
    private final InjectionPoint injectionPoint;

    public EventMetadataImpl(Set<Annotation> qualifiers, Type eventType, InjectionPoint injectionPoint) {
        this.qualifiers = Objects.requireNonNull(qualifiers);
        this.eventType = Objects.requireNonNull(eventType);
        this.injectionPoint = injectionPoint;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public InjectionPoint getInjectionPoint() {
        return injectionPoint;
    }

    @Override
    public Type getType() {
        return eventType;
    }

}
