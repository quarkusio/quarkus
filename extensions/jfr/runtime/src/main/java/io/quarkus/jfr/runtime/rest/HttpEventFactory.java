package io.quarkus.jfr.runtime.rest;

public interface HttpEventFactory {
    <T extends AbstractHttpReactiveStartEvent> T createReactiveStartEvent();

    <T extends AbstractHttpReactiveEndEvent> T createReactiveEndEvent();

    <T extends AbstractHttpBlockingEvent> T createBlockingEvent();
}
