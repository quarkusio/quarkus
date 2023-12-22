package io.quarkus.jfr.runtime.http.rest.tracing;

import jakarta.inject.Singleton;

import io.quarkus.jfr.runtime.http.HttpEventFactory;

@Singleton
public class TracingRestEventFactory implements HttpEventFactory {

    @Override
    public TracingRestReactiveStartEvent createReactiveStartEvent() {
        return new TracingRestReactiveStartEvent();
    }

    @Override
    public TracingRestReactiveEndEvent createReactiveEndEvent() {
        return new TracingRestReactiveEndEvent();
    }

    @Override
    public TracingRestBlockingEvent createBlockingEvent() {
        return new TracingRestBlockingEvent();
    }
}
