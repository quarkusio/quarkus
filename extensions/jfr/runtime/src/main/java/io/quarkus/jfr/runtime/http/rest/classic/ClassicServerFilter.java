package io.quarkus.jfr.runtime.http.rest.classic;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;

@Provider
public class ClassicServerFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(ClassicServerFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Enter Jfr Classic Request Filter");
        }
        ClassicServerRecorder recorder = Arc.container().instance(ClassicServerRecorder.class).get();
        recorder.recordStartEvent();
        recorder.startPeriodEvent();
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Enter Jfr Classic Response Filter");
        }

        if (isRecordable(responseContext)) {
            ClassicServerRecorder recorder = Arc.container().instance(ClassicServerRecorder.class).get();
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
