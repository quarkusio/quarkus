package io.quarkus.opentelemetry.runtime.tracing.intrumentation.resteasy;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

/**
 * Handles RESTEasy Classic
 */
@Provider
public class OpenTelemetryClassicServerFilter implements ContainerRequestFilter {

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Span localRootSpan = LocalRootSpan.current();

        localRootSpan.setAttribute(SemanticAttributes.CODE_NAMESPACE, resourceInfo.getResourceClass().getName());
        localRootSpan.setAttribute(SemanticAttributes.CODE_FUNCTION, resourceInfo.getResourceMethod().getName());
    }
}
