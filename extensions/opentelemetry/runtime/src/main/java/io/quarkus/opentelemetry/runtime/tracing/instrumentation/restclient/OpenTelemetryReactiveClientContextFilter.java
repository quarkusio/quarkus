package io.quarkus.opentelemetry.runtime.tracing.instrumentation.restclient;

import jakarta.annotation.Priority;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import io.opentelemetry.context.Context;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.smallrye.common.vertx.VertxContext;

/**
 * Hands the caller's OpenTelemetry context over to the duplicated Vert.x context the Quarkus REST Client creates for
 * each invocation. This filter does not create any span: with the Quarkus REST Client, tracing is handled by Vert.x.
 * <p>
 * The Vert.x tracer creates the client span when the request head is written, which happens asynchronously on the
 * invocation's Vert.x context and possibly from another event loop. When the caller is on a duplicated context, the
 * client copies its locals (OpenTelemetry context included) to the invocation's context. When the caller is not on a
 * Vert.x context, the OpenTelemetry context lives in a thread local and must be stored explicitly, otherwise the client
 * span starts a new trace. This class performs the bridge.
 * <p>
 * This is deliberately a request filter only, and not part of {@link OpenTelemetryClientFilter}: a
 * {@code ClientResponseFilter} has nothing to do, and because the Quarkus REST Client runs response filters on a worker
 * thread, when the response is handled on an event loop, it would cost a thread switch per call. This strategy is more
 * efficient.
 */
@Priority(Integer.MIN_VALUE)
public class OpenTelemetryReactiveClientContextFilter implements ClientRequestFilter {

    // org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext.VERTX_CONTEXT_PROPERTY
    private static final String VERTX_CONTEXT_PROPERTY = "__context";

    @Override
    public void filter(ClientRequestContext requestContext) {
        Object property = requestContext.getProperty(VERTX_CONTEXT_PROPERTY);
        if (!(property instanceof io.vertx.core.Context)) {
            return;
        }
        io.vertx.core.Context invocationContext = (io.vertx.core.Context) property;
        if (!VertxContext.isDuplicatedContext(invocationContext)
                || QuarkusContextStorage.getOtelContext(invocationContext) != null) {
            return;
        }
        Context callerContext = QuarkusContextStorage.INSTANCE.current();
        if (callerContext != null && callerContext != Context.root()) {
            // The scope is intentionally not closed: the Vert.x context is created for this invocation only and is
            // discarded with it
            QuarkusContextStorage.INSTANCE.attach(invocationContext, callerContext);
        }
    }
}
