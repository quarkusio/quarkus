package io.quarkus.opentelemetry.runtime.tracing.intrumentation.resteasy;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;

public class AttachExceptionHandler implements ServerRestHandler {
    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        Throwable throwable = requestContext.getThrowable();
        if (throwable != null) { // should always be true
            LocalRootSpan.current().recordException(throwable);
        }
    }
}
