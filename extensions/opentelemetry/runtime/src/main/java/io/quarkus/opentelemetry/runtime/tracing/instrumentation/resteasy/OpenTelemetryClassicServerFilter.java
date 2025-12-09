package io.quarkus.opentelemetry.runtime.tracing.instrumentation.resteasy;

import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_FUNCTION_NAME;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;

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

        localRootSpan.setAttribute(CODE_FUNCTION_NAME,
                resourceInfo.getResourceClass().getName() + "." +
                        resourceInfo.getResourceMethod().getName());
    }
}
