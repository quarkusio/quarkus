package io.quarkus.opentelemetry.runtime.tracing.instrumentation.resteasy;

import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_FUNCTION_NAME;

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

        localRootSpan.setAttribute(CODE_FUNCTION_NAME,
                resourceInfo.getResourceClass().getName() + "." + resourceInfo.getMethodName());
    }
}
