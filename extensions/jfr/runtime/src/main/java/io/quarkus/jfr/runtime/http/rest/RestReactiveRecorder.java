package io.quarkus.jfr.runtime.http.rest;

import java.time.Duration;
import java.time.Instant;

import io.quarkus.jfr.runtime.RequestIdProducer;
import io.quarkus.jfr.runtime.http.AbstractHttpBlockingEvent;
import io.quarkus.jfr.runtime.http.AbstractHttpReactiveEndEvent;
import io.quarkus.jfr.runtime.http.AbstractHttpReactiveStartEvent;
import io.quarkus.jfr.runtime.http.HttpEventFactory;
import io.quarkus.logging.Log;

public class RestReactiveRecorder implements RestRecorder {

    private final String httpMethod;
    private final String uri;
    private final String resourceClass;
    private final String resourceMethod;
    private final String client;
    private final RequestIdProducer requestIdProducer;
    private final HttpEventFactory httpEventFactory;
    private AbstractHttpReactiveStartEvent startEvent;
    private AbstractHttpReactiveEndEvent endEvent;
    private AbstractHttpBlockingEvent durationEvent;
    private Instant startTime;

    public RestReactiveRecorder(String httpMethod, String uri, String resourceClass, String resourceMethod, String client,
            RequestIdProducer requestIdProducer, HttpEventFactory httpEventFactory) {
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.resourceClass = resourceClass;
        this.resourceMethod = resourceMethod;
        this.client = client;
        this.requestIdProducer = requestIdProducer;
        this.httpEventFactory = httpEventFactory;
    }

    @Override
    public void recordReactiveRequest() {
        startEvent = httpEventFactory.createReactiveStartEvent();
        startEvent.end();
        startTime = Instant.now();
    }

    @Override
    public void recordBlockingRequest() {
        durationEvent = httpEventFactory.createBlockingEvent();
        durationEvent.begin();
    }

    @Override
    public void recordReactiveResponse() {
        if (startEvent == null) {
            if (Log.isDebugEnabled()) {
                Log.debug("Jfr Response filter was called without recording the reactive request");
            }
            return;
        }

        Instant endTime = Instant.now();
        endEvent = httpEventFactory.createReactiveEndEvent();
        endEvent.end();

        if (startEvent.shouldCommit()) {
            startEvent.setRequestId(requestIdProducer.create());
            startEvent.setHttpMethod(httpMethod);
            startEvent.setUri(uri);
            startEvent.setResourceClass(resourceClass);
            startEvent.setResourceMethod(resourceMethod);
            startEvent.setClient(client);
            startEvent.commit();
        }
        if (endEvent.shouldCommit()) {
            endEvent.setRequestId(requestIdProducer.create());
            endEvent.setProcessDuration(Duration.between(startTime, endTime).toNanos());
            endEvent.commit();
        }
    }

    @Override
    public void recordBlockingResponse() {
        if (durationEvent == null) {
            if (Log.isDebugEnabled()) {
                Log.debug("Jfr Response filter was called without recording the blocking request");
            }
            return;
        }

        if (durationEvent.shouldCommit()) {
            durationEvent.setRequestId(requestIdProducer.create());
            durationEvent.setHttpMethod(httpMethod);
            durationEvent.setUri(uri);
            durationEvent.setResourceClass(resourceClass);
            durationEvent.setResourceMethod(resourceMethod);
            durationEvent.setClient(client);
            durationEvent.commit();
        }
    }
}
