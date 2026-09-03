package io.grpc.override;

import io.grpc.Context;
import io.quarkus.grpc.runtime.GrpcContextLocalsProvider;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;

/**
 * Override gRPC context storage to rely on duplicated context when available.
 */
public class ContextStorageOverride extends Context.Storage {

    private static final ThreadLocal<Context> fallback = new ThreadLocal<>();

    @Override
    public Context doAttach(Context toAttach) {
        Context current = current();
        io.vertx.core.Context dc = Vertx.currentContext();
        if (dc != null && VertxContext.isDuplicatedContext(dc)) {
            GrpcContextLocalsProvider.GRPC_CONTEXT_LOCAL.put(dc, toAttach);
        } else {
            fallback.set(toAttach);
        }
        return current;
    }

    @Override
    public void detach(Context context, Context toRestore) {
        // It is possible that a previous call to detach() removed the local map entry when cleaning up the context.
        // Due to how io.grpc and quarkus-grpc interact, it is possible that this cleanup be followed by detach(C1,C1),
        // in which case we would put a stale context back in the map we just cleaned.
        // To make sure it doesn't happen we return when we encounter detach(C1,C1).
        // detach(ROOT, ROOT) is a special case used by grpc internals and must not be short-circuited.
        if (toRestore == context && context != Context.ROOT) {
            return;
        }
        io.vertx.core.Context dc = Vertx.currentContext();
        if (toRestore != Context.ROOT) {
            if (dc != null && VertxContext.isDuplicatedContext(dc)) {
                GrpcContextLocalsProvider.GRPC_CONTEXT_LOCAL.put(dc, toRestore);
            } else {
                fallback.set(toRestore);
            }
        } else {
            if (dc != null && VertxContext.isDuplicatedContext(dc)) {
                var local = dc.getLocal(VertxContext.DATA_MAP_LOCAL, ConcurrentHashMap::new);
                local.remove(GRPC_CONTEXT);
            } else {
                fallback.set(null);
            }
        }
    }

    @Override
    public Context current() {
        if (VertxContext.isOnDuplicatedContext()) {
            Context current = GrpcContextLocalsProvider.GRPC_CONTEXT_LOCAL.get(Vertx.currentContext());
            if (current == null) {
                return Context.ROOT;
            }
            return current;
        } else {
            Context current = fallback.get();
            if (current == null) {
                return Context.ROOT;
            }
            return current;
        }
    }
}
