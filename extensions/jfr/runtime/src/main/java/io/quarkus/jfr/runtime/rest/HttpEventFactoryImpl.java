package io.quarkus.jfr.runtime.rest;

import jakarta.inject.Singleton;

@Singleton
public class HttpEventFactoryImpl implements HttpEventFactory {

    @Override
    public HttpReactiveStartEvent createReactiveStartEvent() {
        return new HttpReactiveStartEvent();
    }

    @Override
    public HttpReactiveEndEvent createReactiveEndEvent() {
        return new HttpReactiveEndEvent();
    }

    @Override
    public HttpBlockingEvent createBlockingEvent() {
        return new HttpBlockingEvent();
    }
}
