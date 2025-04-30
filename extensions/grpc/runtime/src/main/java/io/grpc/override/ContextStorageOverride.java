package io.grpc.override;

import io.grpc.Context;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;

/**
 * Override gRPC context storage to rely on duplicated context when available.
 */
public class ContextStorageOverride extends Context.Storage {

    private static final ThreadLocal<Context> fallback = new ThreadLocal<>();

    private static final String GRPC_CONTEXT = "GRPC_CONTEXT";

    @Override
    public Context doAttach(Context toAttach) {
        Context current = current();
        io.vertx.core.Context dc = Vertx.currentContext();
        if (dc != null && VertxContext.isDuplicatedContext(dc)) {
            dc.putLocal(GRPC_CONTEXT, toAttach);
        } else {
            fallback.set(toAttach);
        }
        return current;
    }

    @Override
    public void detach(Context context, Context toRestore) {
        io.vertx.core.Context dc = Vertx.currentContext();
        if (toRestore != Context.ROOT) {
            if (dc != null && VertxContext.isDuplicatedContext(dc)) {
                dc.putLocal(GRPC_CONTEXT, toRestore);
            } else {
                fallback.set(toRestore);
            }
        } else {
            if (dc != null && VertxContext.isDuplicatedContext(dc)) {
                // Do nothing - duplicated context are not shared.
            } else {
                fallback.set(null);
            }
        }
    }

    @Override
    public Context current() {
        if (VertxContext.isOnDuplicatedContext()) {
            Context current = Vertx.currentContext().getLocal(GRPC_CONTEXT);
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
