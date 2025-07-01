package io.quarkus.opentelemetry.runtime.tracing.intrumentation.resteasy;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.quarkus.opentelemetry.runtime.tracing.InternalAttributes;

public class TracelessServerHandler implements ServerRestHandler {
    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        Span localRootSpan = LocalRootSpan.current();
        localRootSpan.setAttribute(InternalAttributes.TRACELESS, "true");
    }
}
