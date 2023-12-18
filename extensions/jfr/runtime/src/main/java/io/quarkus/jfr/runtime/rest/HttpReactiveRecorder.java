package io.quarkus.jfr.runtime.rest;

import java.time.Duration;
import java.time.Instant;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;

import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.jfr.runtime.RequestId;
import io.quarkus.jfr.runtime.RequestIdProducer;
import io.quarkus.logging.Log;
import io.vertx.core.http.HttpServerRequest;

@RequestScoped
public class HttpReactiveRecorder implements HttpRecorder {

    @Inject
    @RequestScoped
    ServerRequestContext serverRequestContext;

    @Inject
    @RequestScoped
    protected RequestIdProducer requestIdProducer;

    @Inject
    HttpEventFactory httpEventFactory;

    @Context
    protected HttpServerRequest vertxRequest;

    @Context
    protected ResourceInfo resourceInfo;

    private AbstractHttpReactiveStartEvent startEvent;

    private AbstractHttpReactiveEndEvent endEvent;

    private AbstractHttpBlockingEvent durationEvent;

    private Instant startTime;

    private RequestId requestId;

    private boolean isNonblocking = false;

    @Override
    public void recordRequest() {
        if (serverRequestContext.getResteasyReactiveResourceInfo().isNonBlocking) {
            recordReactiveRequest();
            isNonblocking = true;
        } else {
            recordBlockingRequest();
        }
    }

    private void recordReactiveRequest() {
        if (Log.isDebugEnabled()) {
            Log.debug("Reactive Request");
        }
        requestId = requestIdProducer.create();
        startEvent = httpEventFactory.createReactiveStartEvent();
        startEvent.end();
        startTime = Instant.now();
    }

    private void recordBlockingRequest() {
        if (Log.isDebugEnabled()) {
            Log.debug("Blocking Request");
        }
        requestId = requestIdProducer.create();
        durationEvent = httpEventFactory.createBlockingEvent();
        durationEvent.begin();
    }

    @Override
    public void recordResponse() {
        if (isNonblocking) {
            recordReactiveResponse();
        } else {
            recordBlockingResponse();
        }
    }

    private void recordReactiveResponse() {
        if (Log.isDebugEnabled()) {
            Log.debug("Reactive Response");
        }
        Instant endTime = Instant.now();
        endEvent = httpEventFactory.createReactiveEndEvent();
        endEvent.end();

        if (startEvent.shouldCommit()) {
            startEvent.setRequestId(requestId);
            startEvent.setHttpMethod(vertxRequest.method().name());
            startEvent.setUri(vertxRequest.path());
            startEvent.setResourceClass(resourceInfo.getResourceClass().getName());
            startEvent.setResourceMethod(resourceInfo.getResourceMethod().toGenericString());
            startEvent.setClient(vertxRequest.remoteAddress().toString());
            startEvent.commit();
        }
        if (endEvent.shouldCommit()) {
            endEvent.setRequestId(requestId);
            endEvent.setProcessDuration(Duration.between(startTime, endTime).toNanos());
            endEvent.commit();
        }
    }

    private void recordBlockingResponse() {
        if (Log.isDebugEnabled()) {
            Log.debug("Blocking Response");
        }
        if (durationEvent.shouldCommit()) {
            durationEvent.setRequestId(requestId);
            durationEvent.setHttpMethod(vertxRequest.method().name());
            durationEvent.setUri(vertxRequest.path());
            durationEvent.setResourceClass(resourceInfo.getResourceClass().getName());
            durationEvent.setResourceMethod(resourceInfo.getResourceMethod().toGenericString());
            durationEvent.setClient(vertxRequest.remoteAddress().toString());
            durationEvent.commit();
        }
    }
}
