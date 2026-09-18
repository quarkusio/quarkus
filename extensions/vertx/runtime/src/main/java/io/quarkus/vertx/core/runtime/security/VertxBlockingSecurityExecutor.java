package io.quarkus.vertx.core.runtime.security;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe;
import static io.smallrye.common.vertx.VertxContext.getOrCreateDuplicatedContext;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class VertxBlockingSecurityExecutor implements BlockingSecurityExecutor {

    @Inject
    Vertx vertx;

    @Override
    public <T> Uni<T> executeBlocking(Supplier<? extends T> supplier) {
        return Uni.createFrom().deferred(new Supplier<Uni<? extends T>>() {
            @Override
            public Uni<? extends T> get() {
                Uni<T> uni = Uni.createFrom().item(supplier);
                if (BlockingOperationControl.isBlockingAllowed()) {
                    return uni;
                }
                Context local = getOrCreateDuplicatedContext(vertx);
                setContextSafe(local, true);
                Executor blockingExecutor = new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        local.executeBlocking(new Callable<Void>() {
                            @Override
                            public Void call() {
                                command.run();
                                return null;
                            }
                        }, false);
                    }
                };
                Executor eventLoopExecutor = new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        local.runOnContext(new io.vertx.core.Handler<Void>() {
                            @Override
                            public void handle(Void event) {
                                command.run();
                            }
                        });
                    }
                };
                return uni.runSubscriptionOn(blockingExecutor)
                        .emitOn(eventLoopExecutor);
            }
        });
    }
}
