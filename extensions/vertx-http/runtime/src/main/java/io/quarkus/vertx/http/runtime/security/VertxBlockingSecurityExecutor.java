package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe;
import static io.smallrye.common.vertx.VertxContext.getOrCreateDuplicatedContext;

import java.util.function.Supplier;

import jakarta.inject.Inject;

import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class VertxBlockingSecurityExecutor implements BlockingSecurityExecutor {

    @Inject
    Vertx vertx;

    @Override
    public <T> Uni<T> executeBlocking(Supplier<? extends T> supplier) {
        Context local = getOrCreateDuplicatedContext(vertx);
        setContextSafe(local, true);
        return Uni
                .createFrom()
                .completionStage(
                        local
                                .executeBlocking(new Handler<Promise<T>>() {
                                    @Override
                                    public void handle(Promise<T> promise) {
                                        promise.complete(supplier.get());
                                    }
                                })
                                .toCompletionStage());
    }
}
