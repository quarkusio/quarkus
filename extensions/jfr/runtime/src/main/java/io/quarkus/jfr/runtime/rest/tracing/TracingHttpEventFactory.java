package io.quarkus.jfr.runtime.rest.tracing;

import jakarta.inject.Singleton;

import io.quarkus.jfr.runtime.rest.HttpEventFactory;

@Singleton
public class TracingHttpEventFactory implements HttpEventFactory {
    @Override
    public TracingHttpReactiveStartEvent createReactiveStartEvent() {
        return new TracingHttpReactiveStartEvent();
    }

    @Override
    public TracingHttpReactiveEndEvent createReactiveEndEvent() {
        return new TracingHttpReactiveEndEvent();
    }

    @Override
    public TracingHttpBlockingEvent createBlockingEvent() {
        return new TracingHttpBlockingEvent();
    }
}
