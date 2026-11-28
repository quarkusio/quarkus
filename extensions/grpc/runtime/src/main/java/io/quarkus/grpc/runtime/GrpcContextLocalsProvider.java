package io.quarkus.grpc.runtime;

import io.grpc.Context;
import io.vertx.core.internal.VertxBootstrap;
import io.vertx.core.spi.VertxServiceProvider;
import io.vertx.core.spi.context.storage.ContextLocal;
import io.vertx.ext.web.RoutingContext;

/**
 * Provides ContextLocal storage for gRPC contexts.
 */
public class GrpcContextLocalsProvider implements VertxServiceProvider {

    public static final ContextLocal<Context> GRPC_CONTEXT_LOCAL = ContextLocal.registerLocal(Context.class);
    public static final ContextLocal<RoutingContext> ROUTING_CONTEXT_LOCAL = ContextLocal.registerLocal(RoutingContext.class);
    // Set during interceptCall, cleared by cleanup. While present, ContextStorageOverride.detach()
    // blocks detach(C, ROOT) on this dc so vert.x-grpcio frame teardowns cannot clear the call context.
    public static final ContextLocal<Boolean> GRPC_CALL_IN_PROGRESS_LOCAL = ContextLocal.registerLocal(Boolean.class);
    // Marker: a blocking/virtual worker will handle cleanup for this call.
    // When present, runCleanup on the event loop skips removal entirely.
    public static final ContextLocal<Boolean> GRPC_WORKER_ACTIVE_LOCAL = ContextLocal.registerLocal(Boolean.class);

    @Override
    public void init(VertxBootstrap builder) {
        // ContextLocal registration happens via the static fields above.
    }
}
