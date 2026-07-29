package io.quarkus.vertx.http.runtime.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.quarkus.dev.spi.HotReplacementContext;
import io.vertx.core.Future;
import io.vertx.core.Promise;

class RemoteDevBodySpoolStoreTest {

    @Test
    void closeRemovesAnEmptyDirectoryWithoutTheStoppingWorkerExecutor() throws Exception {
        StoreHarness harness = store();
        RemoteDevBodySpoolStore store = harness.store();
        RemoteDevBodySpoolStore.Spool spool = await(store.create());
        Path directory = store.directory();
        assertThat(directory).isDirectory();
        spool.delete();

        harness.rejectExecution().set(true);
        store.close();

        assertThat(store.directory()).isNull();
        assertThat(directory).doesNotExist();
    }

    @Test
    void shutdownCleanupDoesNotDependOnTheStoppingWorkerExecutor() throws Exception {
        StoreHarness harness = store();
        RemoteDevBodySpoolStore store = harness.store();
        RemoteDevBodySpoolStore.Spool spool = await(store.create());
        Path directory = store.directory();
        assertThat(directory).isDirectory();

        harness.rejectExecution().set(true);
        store.close();
        await(store.delete(spool));

        assertThat(store.directory()).isNull();
        assertThat(directory).doesNotExist();
    }

    @Test
    void queuedCleanupRemovesDirectoryWithoutStoreClose() throws Exception {
        StoreHarness harness = store();
        RemoteDevBodySpoolStore store = harness.store();
        RemoteDevBodySpoolStore.Spool spool = await(store.create());
        Path directory = store.directory();
        harness.deferExecution().set(true);

        Future<Void> deletion = store.delete(spool);

        assertThat(directory).isDirectory();
        harness.deferredExecution().getAndSet(null).run();
        await(deletion);
        assertThat(store.directory()).isNull();
        assertThat(directory).doesNotExist();
    }

    @Test
    void lastCleanupRemovesDirectoryWhileAnotherSpoolKeepsItAlive() throws Exception {
        StoreHarness harness = store();
        RemoteDevBodySpoolStore store = harness.store();
        RemoteDevBodySpoolStore.Spool first = await(store.create());
        RemoteDevBodySpoolStore.Spool second = await(store.create());
        Path directory = store.directory();

        await(store.delete(first));
        assertThat(directory).isDirectory();

        await(store.delete(second));
        assertThat(store.directory()).isNull();
        assertThat(directory).doesNotExist();
    }

    private static StoreHarness store() {
        AtomicBoolean rejectExecution = new AtomicBoolean();
        AtomicBoolean deferExecution = new AtomicBoolean();
        AtomicReference<Runnable> deferredExecution = new AtomicReference<>();
        var owner = new RemoteSyncHandler("secret", ignored -> {
        }, org.mockito.Mockito.mock(HotReplacementContext.class), "/root") {
            @Override
            <T> Future<T> executeBlocking(Callable<T> action) {
                if (rejectExecution.get()) {
                    throw new RejectedExecutionException("worker executor is stopping");
                }
                if (deferExecution.get()) {
                    Promise<T> result = Promise.promise();
                    boolean stored = deferredExecution.compareAndSet(null, () -> {
                        try {
                            result.complete(action.call());
                        } catch (Exception e) {
                            result.fail(e);
                        }
                    });
                    if (!stored) {
                        return Future.failedFuture("Only one deferred action is supported");
                    }
                    return result.future();
                }
                try {
                    return Future.succeededFuture(action.call());
                } catch (Exception e) {
                    return Future.failedFuture(e);
                }
            }
        };
        return new StoreHarness(new RemoteDevBodySpoolStore(owner), rejectExecution, deferExecution, deferredExecution);
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    private record StoreHarness(RemoteDevBodySpoolStore store, AtomicBoolean rejectExecution,
            AtomicBoolean deferExecution, AtomicReference<Runnable> deferredExecution) {
    }
}
