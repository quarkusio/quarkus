package io.quarkus.grpc.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

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
    public static final ContextLocal<Runnable> GRPC_CONTEXT_CLEANUP_LOCAL = ContextLocal.registerLocal(Runnable.class);
    // Per-call flag: true while a blocking/virtual worker is between attach and consumer completion.
    // Prevents the event-loop runCleanup from removing GRPC_CONTEXT_LOCAL while the worker reads it.
    public static final ContextLocal<AtomicBoolean> GRPC_WORKER_ACTIVE_LOCAL = ContextLocal.registerLocal(AtomicBoolean.class);

    @Override
    public void init(VertxBootstrap builder) {
        // ContextLocal registration happens via the static fields above.
    }
}
