package io.quarkus.opentelemetry.runtime.tracing.instrumentation.restclient;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;

import io.opentelemetry.context.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.smallrye.common.vertx.VertxContext;

/**
 * Keeps the caller's OpenTelemetry context on the per-request duplicated Vert.x context.
 * <p>
 * Quarkus REST copies context locals onto that context only when the caller is already on a duplicated context. A call
 * from a plain thread therefore has its context only in the thread-local fallback storage.
 * {@code OpenTelemetryMpContextPropagationProvider} attaches it for the handler dispatch and {@code endContext()}
 * removes it again. An {@code InputStream} body is written after that, so the Vert.x client tracer no longer sees the
 * parent and starts a new trace. A {@code String} body is written before {@code endContext()}, which is why it keeps
 * the parent.
 * <p>
 * Request filters run on the caller thread, before that switch. Attaching here, and not closing the scope, leaves the
 * context on a context that exists only for this request. That matches the local-data copy already done when the caller
 * is on a duplicated context.
 */
@Unremovable
@Provider
public class OpenTelemetryReactiveClientContextFilter implements ClientRequestFilter {

    /**
     * Captured by the Reactive REST Client. Read by name so this filter does not depend on that module.
     */
    private static final String VERTX_CONTEXT_PROPERTY = "__context";

    @Override
    public void filter(ClientRequestContext request) {
        Object property = request.getProperty(VERTX_CONTEXT_PROPERTY);
        if (!(property instanceof io.vertx.core.Context vertxContext) || !VertxContext.isDuplicatedContext(vertxContext)) {
            return;
        }
        if (QuarkusContextStorage.getOtelContext(vertxContext) != null) {
            return;
        }
        Context current = Context.current();
        // Nothing to propagate. Storing the root would hide a context attached later on this request.
        if (current == null || current == Context.root()) {
            return;
        }
        // The scope is not closed. Closing it when the filter returns would drop the parent before an asynchronous
        // body is sent. This duplicated context is not reused for another request.
        QuarkusContextStorage.INSTANCE.attach(vertxContext, current);
    }
}
