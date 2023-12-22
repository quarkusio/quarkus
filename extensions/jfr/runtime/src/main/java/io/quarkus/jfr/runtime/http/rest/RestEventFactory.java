package io.quarkus.jfr.runtime.http.rest;

import jakarta.inject.Singleton;

import io.quarkus.jfr.runtime.http.HttpEventFactory;

@Singleton
public class RestEventFactory implements HttpEventFactory {

    @Override
    public RestReactiveStartEvent createReactiveStartEvent() {
        return new RestReactiveStartEvent();
    }

    @Override
    public RestReactiveEndEvent createReactiveEndEvent() {
        return new RestReactiveEndEvent();
    }

    @Override
    public RestBlockingEvent createBlockingEvent() {
        return new RestBlockingEvent();
    }
}
