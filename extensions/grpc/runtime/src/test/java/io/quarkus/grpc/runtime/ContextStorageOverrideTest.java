package io.quarkus.grpc.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.Context;
import io.grpc.override.ContextStorageOverride;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;

class ContextStorageOverrideTest {

    private ContextStorageOverride storage;
    private Vertx vertx;

    @BeforeEach
    void setUp() {
        storage = new ContextStorageOverride();
        vertx = Vertx.vertx();
        // The fallback ThreadLocal is static, so explicitly clear it between tests
        // by detaching to ROOT (which calls fallback.set(null) on the non-Vert.x path)
        storage.detach(Context.ROOT, Context.ROOT);
    }

    @AfterEach
    void tearDown() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    // --- Non-Vert.x (ThreadLocal fallback) tests ---

    @Test
    void currentWithoutVertxReturnsRoot() {
        assertThat(storage.current()).isSameAs(Context.ROOT);
    }

    @Test
    void doAttachWithoutVertxReturnsPreviousAndStoresNew() {
        Context ctx = Context.ROOT.withValue(Context.key("k"), "v");

        Context previous = storage.doAttach(ctx);

        assertThat(previous).isSameAs(Context.ROOT);
        assertThat(storage.current()).isSameAs(ctx);
    }

    @Test
    void doAttachWithoutVertxChainsContextsCorrectly() {
        Context ctx1 = Context.ROOT.withValue(Context.key("k1"), "v1");
        Context ctx2 = Context.ROOT.withValue(Context.key("k2"), "v2");

        Context prev1 = storage.doAttach(ctx1);
        Context prev2 = storage.doAttach(ctx2);

        assertThat(prev1).isSameAs(Context.ROOT);
        assertThat(prev2).isSameAs(ctx1);
        assertThat(storage.current()).isSameAs(ctx2);
    }

    @Test
    void detachWithoutVertxToRootClearsStorage() {
        Context ctx = Context.ROOT.withValue(Context.key("k"), "v");
        storage.doAttach(ctx);

        storage.detach(ctx, Context.ROOT);

        assertThat(storage.current()).isSameAs(Context.ROOT);
    }

    @Test
    void detachWithoutVertxToPreviousRestoresPrevious() {
        Context ctx1 = Context.ROOT.withValue(Context.key("k1"), "v1");
        Context ctx2 = Context.ROOT.withValue(Context.key("k2"), "v2");
        storage.doAttach(ctx1);
        storage.doAttach(ctx2);

        storage.detach(ctx2, ctx1);

        assertThat(storage.current()).isSameAs(ctx1);
    }

    // --- Vert.x duplicated context tests ---

    @Test
    void currentOnDuplicatedContextReturnsRoot() throws Exception {
        CompletableFuture<Context> result = new CompletableFuture<>();
        io.vertx.core.Context dc = VertxContext.getOrCreateDuplicatedContext(vertx);

        dc.runOnContext(v -> result.complete(storage.current()));

        assertThat(result.get(5, TimeUnit.SECONDS)).isSameAs(Context.ROOT);
    }

    @Test
    void doAttachOnDuplicatedContextStoresContext() throws Exception {
        Context ctx = Context.ROOT.withValue(Context.key("k"), "v");
        CompletableFuture<Context> previousResult = new CompletableFuture<>();
        CompletableFuture<Context> currentResult = new CompletableFuture<>();
        io.vertx.core.Context dc = VertxContext.getOrCreateDuplicatedContext(vertx);

        dc.runOnContext(v -> {
            previousResult.complete(storage.doAttach(ctx));
            currentResult.complete(storage.current());
        });

        assertThat(previousResult.get(5, TimeUnit.SECONDS)).isSameAs(Context.ROOT);
        assertThat(currentResult.get(5, TimeUnit.SECONDS)).isSameAs(ctx);
    }

    @Test
    void detachOnDuplicatedContextToRootClearsStorage() throws Exception {
        Context ctx = Context.ROOT.withValue(Context.key("k"), "v");
        CompletableFuture<Context> result = new CompletableFuture<>();
        io.vertx.core.Context dc = VertxContext.getOrCreateDuplicatedContext(vertx);

        dc.runOnContext(v -> {
            storage.doAttach(ctx);
            storage.detach(ctx, Context.ROOT);
            result.complete(storage.current());
        });

        assertThat(result.get(5, TimeUnit.SECONDS)).isSameAs(Context.ROOT);
    }

    @Test
    void detachOnDuplicatedContextToPreviousRestoresPrevious() throws Exception {
        Context ctx1 = Context.ROOT.withValue(Context.key("k1"), "v1");
        Context ctx2 = Context.ROOT.withValue(Context.key("k2"), "v2");
        CompletableFuture<Context> result = new CompletableFuture<>();
        io.vertx.core.Context dc = VertxContext.getOrCreateDuplicatedContext(vertx);

        dc.runOnContext(v -> {
            storage.doAttach(ctx1);
            storage.doAttach(ctx2);
            storage.detach(ctx2, ctx1);
            result.complete(storage.current());
        });

        assertThat(result.get(5, TimeUnit.SECONDS)).isSameAs(ctx1);
    }

    @Test
    void duplicatedContextDoesNotLeakToNonVertxThread() throws Exception {
        Context ctx = Context.ROOT.withValue(Context.key("k"), "v");
        CompletableFuture<Void> attached = new CompletableFuture<>();
        io.vertx.core.Context dc = VertxContext.getOrCreateDuplicatedContext(vertx);

        dc.runOnContext(v -> {
            storage.doAttach(ctx);
            attached.complete(null);
        });
        attached.get(5, TimeUnit.SECONDS);

        assertThat(storage.current()).isSameAs(Context.ROOT);
    }

    @Test
    void twoDuplicatedContextsAreIsolated() throws Exception {
        Context ctx1 = Context.ROOT.withValue(Context.key("k1"), "v1");
        Context ctx2 = Context.ROOT.withValue(Context.key("k2"), "v2");
        CompletableFuture<Context> dc1Result = new CompletableFuture<>();
        CompletableFuture<Context> dc2Result = new CompletableFuture<>();
        io.vertx.core.Context root = vertx.getOrCreateContext();
        io.vertx.core.Context dc1 = VertxContext.createNewDuplicatedContext(root);
        io.vertx.core.Context dc2 = VertxContext.createNewDuplicatedContext(root);

        dc1.runOnContext(v -> {
            storage.doAttach(ctx1);
            dc1Result.complete(storage.current());
        });
        dc2.runOnContext(v -> {
            storage.doAttach(ctx2);
            dc2Result.complete(storage.current());
        });

        assertThat(dc1Result.get(5, TimeUnit.SECONDS)).isSameAs(ctx1);
        assertThat(dc2Result.get(5, TimeUnit.SECONDS)).isSameAs(ctx2);
    }
}
