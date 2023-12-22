package io.quarkus.jfr.runtime.http.rest;

import jakarta.inject.Inject;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import io.quarkus.logging.Log;

public class JfrRestReactiveFilter {

    @Inject
    RestRecorder httpRecorder;

    @ServerRequestFilter
    public void requestFilter() {
        if (Log.isDebugEnabled()) {
            Log.debug("Enter Jfr Request Filter");
        }
        httpRecorder.recordRequest();
    }

    @ServerResponseFilter
    public void responseFilter() {
        if (Log.isDebugEnabled()) {
            Log.debug("Enter Jfr Response Filter");
        }
        httpRecorder.recordResponse();
    }
}
