package io.quarkus.jfr.runtime.http.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

public class JfrReactiveServerFilter {

    private static final Logger LOG = Logger.getLogger(JfrReactiveServerFilter.class);

    @Inject
    Recorder recorder;

    @ServerRequestFilter
    public void requestFilter() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Enter Jfr Reactive Request Filter");
        }
        recorder.recordStartEvent();
        recorder.startPeriodEvent();
    }

    @ServerResponseFilter
    public void responseFilter(ContainerResponseContext responseContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Enter Jfr Reactive Response Filter");
        }
        if (isRecordable(responseContext)) {
            recorder.endPeriodEvent();
            recorder.recordEndEvent();
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Recording REST event was skipped");
            }
        }
    }

    private boolean isRecordable(ContainerResponseContext responseContext) {
        return responseContext.getStatus() != Response.Status.NOT_FOUND.getStatusCode();
    }
}
