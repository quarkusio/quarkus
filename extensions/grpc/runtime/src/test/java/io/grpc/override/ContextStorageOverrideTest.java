package io.grpc.override;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.Context;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;

class ContextStorageOverrideTest {

    Vertx vertx;
    ContextStorageOverride storage;

    @BeforeEach
    void setup() {
        vertx = Vertx.vertx();
        storage = new ContextStorageOverride();
    }

    @Test
    void testCancelledContextIsClearedWhenDetachingToRootOnDuplicatedContext() throws Exception {
        ContextInternal root = (ContextInternal) vertx.getOrCreateContext();
        io.vertx.core.Context duplicated = VertxContext.getOrCreateDuplicatedContext(root);

        CompletableFuture<Context> result = new CompletableFuture<>();
        duplicated.runOnContext(x -> {
            try {
                Context.CancellableContext cancellable = Context.ROOT.withCancellation();
                Context previous = storage.doAttach(cancellable);
                cancellable.cancel(new RuntimeException("boom"));

                storage.detach(cancellable, previous);
                result.complete(storage.current());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });

        Context current = result.get(5, TimeUnit.SECONDS);
        assertThat(current).isEqualTo(Context.ROOT);
    }
}
