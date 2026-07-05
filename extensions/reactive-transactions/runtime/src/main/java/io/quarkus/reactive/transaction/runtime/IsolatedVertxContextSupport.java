package io.quarkus.reactive.transaction.runtime;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * Runs a {@link Uni} on a dedicated duplicated Vert.x context so that reactive transaction state
 * stored in {@link io.smallrye.common.vertx.ContextLocals} and Hibernate's contextual storage
 * is isolated per reactive pipeline.
 */
public final class IsolatedVertxContextSupport {

    private static final String ERROR_MSG = "@Transactional reactive support requires a safe (isolated) Vert.x sub-context, but the current context hasn't been flagged as such.";

    private IsolatedVertxContextSupport() {
    }

    public static <T> Uni<T> withIsolatedContext(Function<Context, Uni<T>> work) {
        Context parent = Vertx.currentContext();
        if (parent == null) {
            throw new IllegalStateException("No current Vertx context found");
        }
        VertxContextSafetyToggle.validateContextIfExists(ERROR_MSG, ERROR_MSG);

        Context isolated = VertxContext.createNewDuplicatedContext(parent);
        VertxContextSafetyToggle.setContextSafe(isolated, true);

        return Uni.createFrom().emitter(emitter -> {
            AtomicReference<Cancellable> cancellable = new AtomicReference<>();
            isolated.runOnContext(new Handler<Void>() {
                @Override
                public void handle(Void ignored) {
                    cancellable.set(work.apply(isolated).subscribe().with(
                            item -> runOnContext(isolated, () -> emitter.complete(item)),
                            failure -> runOnContext(isolated, () -> emitter.fail(failure))));
                }
            });
            emitter.onTermination(() -> {
                Cancellable subscription = cancellable.get();
                if (subscription != null) {
                    subscription.cancel();
                }
            });
        });
    }

    private static void runOnContext(Context context, Runnable action) {
        if (Vertx.currentContext() == context) {
            action.run();
        } else {
            context.runOnContext(new Handler<Void>() {
                @Override
                public void handle(Void ignored) {
                    action.run();
                }
            });
        }
    }
}
