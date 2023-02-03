package io.quarkus.opentelemetry.runtime.tracing.intrumentation.resteasy;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

/**
 * Handles RESTEasy Reactive (via Vert.x)
 */
@Provider
public class OpenTelemetryReactiveServerFilter implements ContainerRequestFilter {

    @Context
    SimpleResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Span localRootSpan = LocalRootSpan.current();

        localRootSpan.setAttribute(SemanticAttributes.CODE_NAMESPACE, resourceInfo.getResourceClass().getName());
        localRootSpan.setAttribute(SemanticAttributes.CODE_FUNCTION, resourceInfo.getMethodName());
    }
}
