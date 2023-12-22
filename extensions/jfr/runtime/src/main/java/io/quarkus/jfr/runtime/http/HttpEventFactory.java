package io.quarkus.jfr.runtime.http;

public interface HttpEventFactory {

    AbstractHttpReactiveStartEvent createReactiveStartEvent();

    AbstractHttpReactiveEndEvent createReactiveEndEvent();

    AbstractHttpBlockingEvent createBlockingEvent();
}
