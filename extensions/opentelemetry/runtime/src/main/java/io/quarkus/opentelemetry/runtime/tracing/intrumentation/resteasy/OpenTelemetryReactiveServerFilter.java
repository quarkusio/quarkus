package io.quarkus.opentelemetry.runtime.tracing.intrumentation.resteasy;

import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_FUNCTION;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_NAMESPACE;

import java.io.IOException;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;

/**
 * Handles RESTEasy Reactive (via Vert.x)
 */
public class OpenTelemetryReactiveServerFilter {

    @ServerRequestFilter
    public void filter(SimpleResourceInfo resourceInfo) throws IOException {
        Span localRootSpan = LocalRootSpan.current();

        localRootSpan.setAttribute(CODE_NAMESPACE, resourceInfo.getResourceClass().getName());
        localRootSpan.setAttribute(CODE_FUNCTION, resourceInfo.getMethodName());
    }
}
