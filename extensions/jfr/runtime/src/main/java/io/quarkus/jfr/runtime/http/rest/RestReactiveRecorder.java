package io.quarkus.jfr.runtime.http.rest;

import java.time.Duration;
import java.time.Instant;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;

import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.jfr.runtime.RequestIdProducer;
import io.quarkus.jfr.runtime.http.AbstractHttpBlockingEvent;
import io.quarkus.jfr.runtime.http.AbstractHttpReactiveEndEvent;
import io.quarkus.jfr.runtime.http.AbstractHttpReactiveStartEvent;
import io.quarkus.jfr.runtime.http.HttpEventFactory;
import io.quarkus.logging.Log;
import io.vertx.core.http.HttpServerRequest;

@RequestScoped
public class RestReactiveRecorder implements RestRecorder {

    @Inject
    RequestIdProducer requestIdProducer;

    @Context
    HttpServerRequest vertxRequest;

    @Context
    ResourceInfo resourceInfo;

    @Inject
    ServerRequestContext serverRequestContext;

    @Inject
    HttpEventFactory httpEventFactory;

    private AbstractHttpReactiveStartEvent startEvent;
    private AbstractHttpReactiveEndEvent endEvent;
    private AbstractHttpBlockingEvent durationEvent;
    private Instant startTime;

    @Override
    public void recordRequest() {
        if (serverRequestContext.getResteasyReactiveResourceInfo().isNonBlocking) {
            if (Log.isDebugEnabled()) {
                Log.debug("Starting recording reactive request");
            }
            recordReactiveRequest();
        } else {
            if (Log.isDebugEnabled()) {
                Log.debug("Starting recording blocking request");
            }
            recordBlockingRequest();
        }
    }

    private void recordReactiveRequest() {
        startEvent = httpEventFactory.createReactiveStartEvent();
        startEvent.end();
        startTime = Instant.now();
    }

    private void recordBlockingRequest() {
        durationEvent = httpEventFactory.createBlockingEvent();
        durationEvent.begin();
    }

    @Override
    public void recordResponse() {
        if (serverRequestContext.getResteasyReactiveResourceInfo() == null) {
            if (Log.isDebugEnabled()) {
                Log.debug("Skipped recording because ResteasyReactiveResourceInfo is null");
            }
            return;
        }

        if (serverRequestContext.getResteasyReactiveResourceInfo().isNonBlocking) {
            if (Log.isDebugEnabled()) {
                Log.debug("Finishing recording reactive request");
            }

            recordReactiveResponse();
        } else {
            if (Log.isDebugEnabled()) {
                Log.debug("Finishing recording blocking request");
            }

            recordBlockingResponse();
        }
    }

    private void recordReactiveResponse() {
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
            startEvent.setHttpMethod(vertxRequest.method().name());
            startEvent.setUri(vertxRequest.path());
            startEvent.setResourceClass(resourceInfo.getResourceClass().getName());
            startEvent.setResourceMethod(resourceInfo.getResourceMethod().toGenericString());
            startEvent.setClient(vertxRequest.remoteAddress().toString());
            startEvent.commit();
        }
        if (endEvent.shouldCommit()) {
            endEvent.setRequestId(requestIdProducer.create());
            endEvent.setProcessDuration(Duration.between(startTime, endTime).toNanos());
            endEvent.commit();
        }
    }

    private void recordBlockingResponse() {
        if (durationEvent == null) {
            if (Log.isDebugEnabled()) {
                Log.debug("Jfr Response filter was called without recording the blocking request");
            }
            return;
        }

        if (durationEvent.shouldCommit()) {
            durationEvent.setRequestId(requestIdProducer.create());
            durationEvent.setHttpMethod(vertxRequest.method().name());
            durationEvent.setUri(vertxRequest.path());
            durationEvent.setResourceClass(resourceInfo.getResourceClass().getName());
            durationEvent.setResourceMethod(resourceInfo.getResourceMethod().toGenericString());
            durationEvent.setClient(vertxRequest.remoteAddress().toString());
            durationEvent.commit();
        }
    }
}
