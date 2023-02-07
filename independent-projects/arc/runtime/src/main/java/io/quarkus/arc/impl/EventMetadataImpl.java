package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.enterprise.inject.spi.InjectionPoint;

// this class is public because it is used in io.quarkus.arc.InjectableObserverMethod
public final class EventMetadataImpl implements EventMetadata {

    private final Set<Annotation> qualifiers;

    private final Type eventType;

    public EventMetadataImpl(Set<Annotation> qualifiers, Type eventType) {
        this.qualifiers = qualifiers;
        this.eventType = eventType;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public InjectionPoint getInjectionPoint() {
        // Currently we do not support injection point of the injected Event instance which fired the event
        return null;
    }

    @Override
    public Type getType() {
        return eventType;
    }

}
