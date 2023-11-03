package io.quarkus.arc.impl;

import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.EventMetadata;

// this class is public because it is used in io.quarkus.arc.InjectableObserverMethod
public final class EventContextImpl<T> implements EventContext<T> {

    private final T payload;

    private final EventMetadata metadata;

    public EventContextImpl(T payload, EventMetadata metadata) {
        this.payload = payload;
        this.metadata = metadata;
    }

    @Override
    public T getEvent() {
        return payload;
    }

    @Override
    public EventMetadata getMetadata() {
        return metadata;
    }

}
