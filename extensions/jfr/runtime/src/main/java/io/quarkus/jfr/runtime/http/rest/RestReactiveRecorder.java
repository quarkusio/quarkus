package io.quarkus.jfr.runtime.http.rest;

import java.time.Duration;
import java.time.Instant;

import io.quarkus.jfr.runtime.IdProducer;
import io.quarkus.logging.Log;

public class RestReactiveRecorder implements RestRecorder {

    private final String httpMethod;
    private final String uri;
    private final String resourceClass;
    private final String resourceMethod;
    private final String client;
    private final IdProducer idProducer;
    private RestReactiveStartEvent startEvent;
    private RestReactiveEndEvent endEvent;
    private RestBlockingEvent durationEvent;
    private Instant startTime;

    public RestReactiveRecorder(String httpMethod, String uri, String resourceClass, String resourceMethod, String client,
            IdProducer idProducer) {
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.resourceClass = resourceClass;
        this.resourceMethod = resourceMethod;
        this.client = client;
        this.idProducer = idProducer;
    }

    @Override
    public void recordReactiveRequest() {
        startEvent = new RestReactiveStartEvent();
        startEvent.end();
        startTime = Instant.now();
    }

    @Override
    public void recordBlockingRequest() {
        durationEvent = new RestBlockingEvent();
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
        endEvent = new RestReactiveEndEvent();
        endEvent.end();

        if (startEvent.shouldCommit()) {
            startEvent.setTraceId(idProducer.getTraceId());
            startEvent.setSpanId(idProducer.getSpanId());
            startEvent.setHttpMethod(httpMethod);
            startEvent.setUri(uri);
            startEvent.setResourceClass(resourceClass);
            startEvent.setResourceMethod(resourceMethod);
            startEvent.setClient(client);
            startEvent.commit();
        }
        if (endEvent.shouldCommit()) {
            endEvent.setTraceId(idProducer.getTraceId());
            endEvent.setSpanId(idProducer.getSpanId());
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
            durationEvent.setTraceId(idProducer.getTraceId());
            durationEvent.setSpanId(idProducer.getSpanId());
            durationEvent.setHttpMethod(httpMethod);
            durationEvent.setUri(uri);
            durationEvent.setResourceClass(resourceClass);
            durationEvent.setResourceMethod(resourceMethod);
            durationEvent.setClient(client);
            durationEvent.commit();
        }
    }
}
