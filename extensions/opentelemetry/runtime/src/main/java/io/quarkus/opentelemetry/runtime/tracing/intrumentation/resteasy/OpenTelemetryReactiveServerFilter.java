package io.quarkus.opentelemetry.runtime.tracing.intrumentation.resteasy;

import java.io.IOException;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.semconv.SemanticAttributes;

/**
 * Handles RESTEasy Reactive (via Vert.x)
 */
public class OpenTelemetryReactiveServerFilter {

    @ServerRequestFilter
    public void filter(SimpleResourceInfo resourceInfo) throws IOException {
        Span localRootSpan = LocalRootSpan.current();

        localRootSpan.setAttribute(SemanticAttributes.CODE_NAMESPACE, resourceInfo.getResourceClass().getName());
        localRootSpan.setAttribute(SemanticAttributes.CODE_FUNCTION, resourceInfo.getMethodName());
    }
}
