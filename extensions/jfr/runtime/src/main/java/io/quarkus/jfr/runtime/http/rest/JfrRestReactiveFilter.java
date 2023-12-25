package io.quarkus.jfr.runtime.http.rest;

import jakarta.inject.Inject;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.logging.Log;

public class JfrRestReactiveFilter {

    @Inject
    ServerRequestContext serverRequestContext;

    @Inject
    RestRecorder httpRecorder;

    @ServerRequestFilter
    public void requestFilter() {
        if (Log.isDebugEnabled()) {
            Log.debug("Enter Jfr Request Filter");
        }
        if (serverRequestContext.getResteasyReactiveResourceInfo().isNonBlocking) {
            httpRecorder.recordReactiveRequest();
        } else {
            httpRecorder.recordBlockingRequest();
        }
    }

    @ServerResponseFilter
    public void responseFilter() {
        if (Log.isDebugEnabled()) {
            Log.debug("Enter Jfr Response Filter");
        }
        if (serverRequestContext.getResteasyReactiveResourceInfo() == null) {
            if (Log.isDebugEnabled()) {
                Log.debug("Skipped recording because ResteasyReactiveResourceInfo is null");
            }
            return;
        }
        if (serverRequestContext.getResteasyReactiveResourceInfo().isNonBlocking) {
            httpRecorder.recordReactiveResponse();
        } else {
            httpRecorder.recordBlockingResponse();
        }
    }
}
