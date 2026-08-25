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

    @Override
    public void init(VertxBootstrap builder) {
        // ContextLocal registration happens via the static fields above.
    }
}
